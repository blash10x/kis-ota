package blash10x.kis.ota.core.util;

import java.lang.reflect.Field;

/**
 * @author myungsik.sung@gmail.com
 */
public abstract class Reflections {

  private static Field getField(String fieldName, Class<?> type) {
    Class<?> currentClass = type;
    while (currentClass != null) {
      try {
        Field field = currentClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
      } catch (NoSuchFieldException ignored) {
      }
      currentClass = currentClass.getSuperclass();
    }
    return null;
  }

  public static Object getFieldValue(String fieldName, Object object) {
    if (fieldName != null) {
      Field field = getField(fieldName, object.getClass());
      return getFieldValue(field, object);
    }
    return null;
  }

  public static Object getFieldValue(Field field, Object object) {
    if (field != null) {
      field.setAccessible(true);
      try {
        return field.get(object);
      } catch (IllegalAccessException e) {
        doThrow(e);
      }
    }
    return null;
  }

  public static void setFieldValue(Field field, Object object, Object value) {
    if (field != null) {
      field.setAccessible(true);
      try {
        field.set(object, value);
      } catch (IllegalAccessException e) {
        doThrow(e);
      }
    }
  }

  public static void setFieldValue(String fieldName, Object object, Object value) {
    Field field = getField(fieldName, object.getClass());
    setFieldValue(field, object, value);
  }

  @SuppressWarnings("unchecked")
  public static <E extends Exception> void doThrow(Exception e) throws E {
    throw (E) e;
  }
}
