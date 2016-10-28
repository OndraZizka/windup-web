package org.jboss.windup.web.addons.tsmodelsgen;

import org.apache.commons.lang3.ObjectUtils;

/**
 * @author <a href="mailto:jesse.sightler@gmail.com">Jesse Sightler</a>
 */
interface ModelType
{
    static ModelType from(Class cls)
    {
        return ObjectUtils.defaultIfNull(FrameType.from(cls), PrimitiveType.from(cls));
    }

    String getTypeScriptTypeName();
}
