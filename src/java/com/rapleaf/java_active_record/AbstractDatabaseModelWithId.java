package com.rapleaf.java_active_record;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

public abstract class AbstractDatabaseModelWithId<T extends ModelWithId> implements IModelPersistence<T> {
  protected static interface AttrSetter {
    public void set(PreparedStatement stmt) throws SQLException;
  }

  private final DatabaseConnection conn;
  private final String tableName;

  protected final Map<Long, T> cachedById = new HashMap<Long, T>();

  protected final Map<String, Map<Integer, List<T>>> belongsToCache = new HashMap<String, Map<Integer, List<T>>>();
  protected final Map<String, SerializableMethod> belongsToAssociations = new HashMap<String, SerializableMethod>();

  protected final Map<String, Map<Integer, List<T>>> integerIndexCache = new HashMap<String, Map<Integer, List<T>>>();
  private final List<String> fieldNames;
  private final String updateStatement;

  protected AbstractDatabaseModelWithId(DatabaseConnection conn, String tableName, List<String> fieldNames) {
    this.conn = conn;
    this.tableName = tableName;
    this.fieldNames = fieldNames;
    updateStatement = String.format("UPDATE %s SET %s WHERE id = ?;", tableName, getSetFieldsPrepStatementSection());
  }

  protected String getInsertStatement(List<String> fieldNames) {
    return String.format("INSERT INTO %s (%s) VALUES(%s);",
        tableName, escapedFieldNames(fieldNames), qmarks(fieldNames.size()));
  }

  private String getSetFieldsPrepStatementSection() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fieldNames.size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append("`").append(fieldNames.get(i)).append("` = ?");
    }
    return sb.toString();
  }

  protected DatabaseConnection getConn() {
    return conn;
  }

  protected abstract T instanceFromResultSet(ResultSet rs) throws SQLException;

  protected long realCreate(AttrSetter attrSetter, String insertStatement) throws IOException {
    PreparedStatement stmt = conn.getPreparedStatement(insertStatement);
    try {
      attrSetter.set(stmt);
      stmt.execute();
      ResultSet generatedKeys = stmt.getGeneratedKeys();
      generatedKeys.next();
      long newId = generatedKeys.getLong(1);
      return newId;
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  private String escapedFieldNames(List<String> fieldNames) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < fieldNames.size(); i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append("`").append(fieldNames.get(i)).append("`");
    }
    return sb.toString();
  }

  public T find(long id) throws IOException {
    T model = cachedById.get(id);
    if (model != null) {
      return model;
    }

    try {
      ResultSet rs = conn.getPreparedStatement("SELECT * FROM " + tableName + " WHERE id=" + id).executeQuery();
      model = rs.next() ? instanceFromResultSet(rs) : null;
    } catch (SQLException e) {
      throw new IOException(e);
    }

    cachedById.put(id, model);
    return model;
  }

  protected PreparedStatement getSaveStmt() {
    return conn.getPreparedStatement(updateStatement);
  }

//  protected void addModelToPersistenceCache(T1 model) {
//    for(Map.Entry<String, SerializableMethod> entry : belongsToAssociations.entrySet()) {
//      Map<Integer, List<T1>> cache = belongsToCache.get(entry.getKey());
//      Integer id;
//      try {
//        id = (Integer) entry.getValue().getMethod().invoke(model, (Object[])null);
//      } catch (Exception e) {
//        throw new RuntimeException(e);
//      }
//      if (!cache.containsKey(id)) {
//        cache.put(id, new ArrayList<T1>());
//      }
//      cache.get(id).add(model);
//    }
//  }
  
  public List<T> getAllByForeignKey(String foreignKey, int id) throws IOException {
    return getAllByKey(foreignKey, id, belongsToCache);
  }

  public List<T> getAllByIntegerIndexKey(String index, int value) throws IOException {
    return getAllByKey(index, value, integerIndexCache);
  }

  private List<T> getAllByKey(String index, int value, Map<String, Map<Integer, List<T>>> cacheMap) throws IOException {
    if(!cacheMap.containsKey(index)) {
      throw new IllegalArgumentException("No such index or foreign key " + index);
    }

    Map<Integer, List<T>> cache = cacheMap.get(index);

    List<T> result= cache.get(value);
    if(result != null) {
      return Collections.unmodifiableList(result);
    }

    result = new ArrayList<T>();

    try {
      ResultSet rs = conn.getPreparedStatement("SELECT * FROM " + tableName + " WHERE " + index + "=" + value).executeQuery();
      while(rs.next()) {
        T model = instanceFromResultSet(rs);
        if (model != null) {
          result.add(model);
        }
      }
      rs.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }

    cache.put(value, result);
    return Collections.unmodifiableList(result);
  }

  public void clearCacheByForeignKey(String foreignKey, int id) throws IOException {
    throw new NotImplementedException();
//    clearCacheByKey(foreignKey, id, belongsToCache);
  }

  public void clearCacheByIntegerIndexKey(String index, int value) throws IOException {
    throw new NotImplementedException();
//    clearCacheByKey(index, value, integerIndexCache);
  }

//  private void clearCacheByKey(String index, int value, Map<String, Map<Integer, List<T>>> cacheMap) throws IOException {
//    if (!cacheMap.containsKey(index)) {
//      throw new IllegalArgumentException("No such index or foreign key");
//    }
//
//    cacheMap.get(index).remove(value);
//  }

  protected final static Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
    Integer value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  protected final static Long getLongOrNull(ResultSet rs, String column) throws SQLException {
    Long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  protected final static Date getDate(ResultSet rs, String column) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(column);
    if (timestamp == null) {
      return null;
    }
    return new Date(timestamp.getTime());
  }

  protected final static Long getDateAsLong(ResultSet rs, String column ) throws SQLException {
    Date date = getDate(rs, column);
    return date == null ? null : date.getTime();
  }

  @Override
  public void clearCacheByForeignKey(String foreignKey, long id) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void clearCacheById(long id) throws IOException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Set<T> findAllByForeignKey(String foreignKey, long id) throws IOException {
    PreparedStatement stmt = conn.getPreparedStatement(String.format("SELECT * FROM %s WHERE %s = %d;", tableName, foreignKey, id));
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery();
      Set<T> ret = new HashSet<T>();
      while (rs.next()) {
        ret.add(instanceFromResultSet(rs));
      }
      return ret;
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
      } catch (SQLException e) {
        throw new IOException(e);
      }
    }
  }

  protected abstract void setAttrs(T model, PreparedStatement stmt) throws SQLException;

  @Override
  public boolean save(T model) throws IOException {
    PreparedStatement saveStmt = getSaveStmt();
    try {
      setAttrs(model, saveStmt);
      saveStmt.execute();
      return saveStmt.getUpdateCount() == 1;
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  private static String qmarks(int size) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < size; i++) {
      if (i != 0) {
        sb.append(", ");
      }
      sb.append("?");
    }
    return sb.toString();
  }

  public String getTableName() {
    return tableName;
  }

  @Override
  public boolean delete(long id) throws IOException {
    try {
      cachedById.remove(id);
      return conn.getPreparedStatement(String.format("DELETE FROM %s WHERE id=%d", tableName, id)).executeUpdate() == 1;
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public boolean delete(T model) throws IOException {
    return delete(model.getId());
  }

  @Override
  public boolean deleteAll() throws IOException {
    try {
      return conn.getPreparedStatement(String.format("TRUNCATE TABLE %s", tableName)).executeUpdate() >= 0;
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Set<T> findAll() throws IOException {
    return findAll("true");
  }

  @Override
  public Set<T> findAll(String conditions) throws IOException {
    PreparedStatement stmt = conn.getPreparedStatement("SELECT * FROM " + getTableName() + " WHERE " + conditions + ";");
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery();

      Set<T> results = new HashSet<T>();
      while (rs.next()) {
        T inst = instanceFromResultSet(rs);
        cachedById.put(inst.getId(), inst);
        results.add(inst);
      }
      return results;
    } catch (SQLException e) {
      throw new IOException(e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          throw new IOException(e);
        }
      }
    }
  }
}