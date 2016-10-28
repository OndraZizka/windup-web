package org.jboss.windup.web.addons.tsmodelsgen;

/**
 * Handles String, boolean, byte, char, short, int, long, float, and double.
 *
 * @author <a href="http://ondra.zizka.cz/">Ondrej Zizka, zizka@seznam.cz</a>
 */
enum PrimitiveType implements ModelType
{
    STRING("string"), NUMBER("number"), BOOLEAN("boolean"), ENUM("string"), ANY("any");

    private String typeScriptTypeName;

    PrimitiveType(String tsType)
    {
        this.typeScriptTypeName = tsType;
    }

    static PrimitiveType from(Class type)
    {
        if (Iterable.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Given type is Iterable (not a primitive type): " + type.getName());
        else if (String.class.isAssignableFrom(type))
            return STRING;
        else if (Number.class.isAssignableFrom(type) || type.equals(Integer.TYPE) || type.equals(Long.TYPE) || type.equals(Double.TYPE)
                || type.equals(Short.TYPE) || type.equals(Float.TYPE) || type.equals(Byte.TYPE) || type.equals(Character.TYPE)
                || type.equals(Byte.TYPE))
            return NUMBER;
        else if (Boolean.class.isAssignableFrom(type) || type.equals(Boolean.TYPE))
            return BOOLEAN;
        else if (Enum.class.isAssignableFrom(type))
            return ENUM;
        else
            return ANY;
    }

    @Override
    public String getTypeScriptTypeName()
    {
        return typeScriptTypeName;
    }
}
