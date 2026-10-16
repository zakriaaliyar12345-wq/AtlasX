package com.atlasx.gis.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.atlasx.gis.data.model.Project;
import com.atlasx.gis.data.model.SavedPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Local SQLite storage for Atlas X.
 *
 * <p>Schema version 2 added the project reference and timestamps to the points
 * table of version 1; version 3 created the projects table that version 1 did
 * not have. Every migration is purely additive, so existing user data is
 * preserved and no destructive upgrade is performed.</p>
 */
public class AtlasDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "atlasx_gis.db";
    private static final int DATABASE_VERSION = 3;

    private static final String T_POINTS = "points";
    private static final String T_PROJECTS = "projects";

    private static AtlasDatabase instance;

    private final Context appContext;

    private AtlasDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.appContext = context.getApplicationContext();
    }

    /** Process-wide singleton so every screen shares one connection. */
    public static synchronized AtlasDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new AtlasDatabase(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_PROJECTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE " + T_POINTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "latitude REAL NOT NULL," +
                "longitude REAL NOT NULL," +
                "description TEXT NOT NULL DEFAULT ''," +
                "project_id INTEGER," +
                "created_at INTEGER NOT NULL DEFAULT 0," +
                "updated_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_points_name ON " + T_POINTS + "(name)");
        db.execSQL("CREATE INDEX idx_points_project ON " + T_POINTS + "(project_id)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            if (!hasColumn(db, T_POINTS, "project_id")) {
                db.execSQL("ALTER TABLE " + T_POINTS + " ADD COLUMN project_id INTEGER");
            }
            if (!hasColumn(db, T_POINTS, "created_at")) {
                db.execSQL("ALTER TABLE " + T_POINTS +
                        " ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0");
            }
            if (!hasColumn(db, T_POINTS, "updated_at")) {
                db.execSQL("ALTER TABLE " + T_POINTS +
                        " ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0");
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_points_project ON " +
                    T_POINTS + "(project_id)");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + T_PROJECTS + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "created_at INTEGER NOT NULL)");
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Never destroy user data when a newer database is opened by an older
        // build; keep the existing schema and let queries fail loudly instead.
        android.util.Log.w("AtlasX", "Database downgrade requested: " +
                oldVersion + " -> " + newVersion + " (ignored)");
    }

    private static boolean hasColumn(SQLiteDatabase db, String table, String column) {
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIndex = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(cursor.getString(nameIndex))) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- points

    public long insertPoint(SavedPoint point) {
        long now = System.currentTimeMillis();
        ContentValues values = pointValues(point);
        values.put("created_at", now);
        values.put("updated_at", now);
        long id = getWritableDatabase().insertOrThrow(T_POINTS, null, values);
        point.setId(id);
        return id;
    }

    public boolean updatePoint(SavedPoint point) {
        ContentValues values = pointValues(point);
        values.put("updated_at", System.currentTimeMillis());
        int rows = getWritableDatabase().update(
                T_POINTS, values, "id=?", new String[]{String.valueOf(point.getId())});
        return rows > 0;
    }

    public boolean deletePoint(long id) {
        int rows = getWritableDatabase().delete(
                T_POINTS, "id=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    private static ContentValues pointValues(SavedPoint point) {
        ContentValues values = new ContentValues();
        values.put("name", point.getName());
        values.put("latitude", point.getLatitude());
        values.put("longitude", point.getLongitude());
        values.put("description", point.getDescription());
        if (point.getProjectId() > 0) {
            values.put("project_id", point.getProjectId());
        } else {
            values.putNull("project_id");
        }
        return values;
    }

    private static final String[] POINT_COLUMNS = {
            "id", "name", "latitude", "longitude", "description",
            "project_id", "created_at", "updated_at"
    };

    public List<SavedPoint> getPoints(String search, Long projectId, int limit) {
        List<SavedPoint> result = new ArrayList<>();
        List<String> args = new ArrayList<>();
        StringBuilder where = new StringBuilder();

        if (search != null && !search.trim().isEmpty()) {
            where.append("(name LIKE ? OR description LIKE ?)");
            String like = "%" + search.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (projectId != null && projectId > 0) {
            if (where.length() > 0) where.append(" AND ");
            where.append("project_id = ?");
            args.add(String.valueOf(projectId));
        }

        String sql = "SELECT " + String.join(",", POINT_COLUMNS) + " FROM " + T_POINTS;
        if (where.length() > 0) sql += " WHERE " + where;
        sql += " ORDER BY id DESC";
        if (limit > 0) sql += " LIMIT " + limit;

        try (Cursor cursor = getReadableDatabase().rawQuery(
                sql, args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                result.add(readPoint(cursor));
            }
        }
        return result;
    }

    public List<SavedPoint> getPoints() {
        return getPoints(null, null, 0);
    }

    public SavedPoint getPoint(long id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT " + String.join(",", POINT_COLUMNS) + " FROM " + T_POINTS +
                        " WHERE id=?", new String[]{String.valueOf(id)})) {
            return cursor.moveToFirst() ? readPoint(cursor) : null;
        }
    }

    private static SavedPoint readPoint(Cursor c) {
        return new SavedPoint(
                c.getLong(0),
                c.getString(1),
                c.getDouble(2),
                c.getDouble(3),
                c.isNull(4) ? "" : c.getString(4),
                c.isNull(5) ? 0L : c.getLong(5),
                c.getLong(6),
                c.getLong(7));
    }

    public int countPoints() {
        return count("SELECT COUNT(*) FROM " + T_POINTS);
    }

    public int countPointsInProject(long projectId) {
        return count("SELECT COUNT(*) FROM " + T_POINTS + " WHERE project_id=?",
                String.valueOf(projectId));
    }

    // -------------------------------------------------------------- projects

    public long insertProject(String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insertOrThrow(T_PROJECTS, null, values);
    }

    public boolean renameProject(long id, String name) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        return getWritableDatabase().update(
                T_PROJECTS, values, "id=?", new String[]{String.valueOf(id)}) > 0;
    }

    /**
     * Deletes a project. Points keep existing rows and simply lose their
     * project reference ({@code ON DELETE SET NULL} behaviour done in code so
     * the intent is explicit and testable).
     */
    public boolean deleteProject(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues clear = new ContentValues();
            clear.putNull("project_id");
            db.update(T_POINTS, clear, "project_id=?", new String[]{String.valueOf(id)});
            int rows = db.delete(T_PROJECTS, "id=?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
            return rows > 0;
        } finally {
            db.endTransaction();
        }
    }

    public List<Project> getProjects() {
        List<Project> result = new ArrayList<>();
        String sql = "SELECT p.id, p.name, p.created_at, " +
                "(SELECT COUNT(*) FROM " + T_POINTS + " x WHERE x.project_id = p.id) " +
                "FROM " + T_PROJECTS + " p ORDER BY p.id DESC";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                result.add(new Project(
                        cursor.getLong(0),
                        cursor.getString(1),
                        cursor.getLong(2),
                        cursor.getInt(3)));
            }
        }
        return result;
    }

    public boolean projectNameExists(String name) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + T_PROJECTS +
                        " WHERE LOWER(name) = LOWER(?)", new String[]{name.trim()})) {
            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        }
    }

    public int countProjects() {
        return count("SELECT COUNT(*) FROM " + T_PROJECTS);
    }

    private int count(String sql, String... args) {
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    /** Size on disk of the database file (and its write-ahead log, if any). */
    public long databaseSizeBytes() {
        long total = 0;
        java.io.File db = appContext.getDatabasePath(DATABASE_NAME);
        java.io.File wal = new java.io.File(db.getPath() + "-wal");
        java.io.File shm = new java.io.File(db.getPath() + "-shm");
        if (db.exists()) total += db.length();
        if (wal.exists()) total += wal.length();
        if (shm.exists()) total += shm.length();
        return total;
    }
}
