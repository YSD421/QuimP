package com.github.celldynamics.quimp.plugin.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Extension of LinkedHashMap that assumes that Key is String and it is case insensitive.
 * 
 * <p>All keys are converted to lower case.
 * 
 * @author p.baniukiewicz
 * @param <V>
 *
 */
public class LinkedStringMap<V> extends LinkedHashMap<String, V> {

  private static final long serialVersionUID = -8577387803055420569L;

  /**
   * Default constructor.
   */
  public LinkedStringMap() {
  }

  /**
   * LinkedStringMap.
   * 
   * @param initialCapacity initialCapacity
   */
  public LinkedStringMap(int initialCapacity) {
    super(initialCapacity);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
   */
  @Override
  public V put(String key, V value) {
    return super.put(key.toLowerCase(), value);
  }

  /**
   * Get the value of key.
   * 
   * @param key key
   * @return value for key
   * @see java.util.HashMap#get(java.lang.Object)
   */
  public V get(String key) {
    return super.get(key.toLowerCase());
  }

  /**
   * containsKey.
   * 
   * @param key key
   * @return true if contains key
   * @see java.util.HashMap#containsKey(java.lang.Object)
   */
  public boolean containsKey(String key) {
    return super.containsKey(key.toLowerCase());
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.HashMap#putAll(java.util.Map)
   */
  @Override
  public void putAll(Map<? extends String, ? extends V> m) {
    for (Map.Entry<? extends String, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  /**
   * Remove key.
   * 
   * @param key key
   * @return value removed
   * @see java.util.HashMap#remove(java.lang.Object)
   */
  public V remove(String key) {
    return super.remove(key.toLowerCase());
  }

  /**
   * Remove key.
   * 
   * @param key key
   * @param value value
   * @return true if removed
   * @see java.util.HashMap#remove(java.lang.Object, java.lang.Object)
   */
  public boolean remove(String key, Object value) {
    return super.remove(key.toLowerCase(), value);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.HashMap#putIfAbsent(java.lang.Object, java.lang.Object)
   */
  @Override
  public V putIfAbsent(String key, V value) {
    return super.putIfAbsent(key.toLowerCase(), value);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.HashMap#replace(java.lang.Object, java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean replace(String key, V oldValue, V newValue) {
    return super.replace(key.toLowerCase(), oldValue, newValue);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.HashMap#replace(java.lang.Object, java.lang.Object)
   */
  @Override
  public V replace(String key, V value) {
    return super.replace(key.toLowerCase(), value);
  }

  /**
   * This method is not supported.
   * 
   * @see java.util.HashMap#replaceAll(java.util.function.BiFunction)
   */
  @Override
  public void replaceAll(BiFunction<? super String, ? super V, ? extends V> function) {
    throw new UnsupportedOperationException("not supported");
  }

}
