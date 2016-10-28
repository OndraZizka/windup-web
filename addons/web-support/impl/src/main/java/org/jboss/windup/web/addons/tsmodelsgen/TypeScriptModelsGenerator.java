package org.jboss.windup.web.addons.tsmodelsgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.windup.graph.model.WindupFrame;
import org.jboss.windup.graph.model.WindupVertexFrame;
import org.jboss.windup.web.addons.websupport.tsmodelgen.TypeScriptModelsGeneratorConfig;
import org.jboss.windup.web.addons.websupport.tsmodelgen.TypeScriptModelsGeneratorConfig.AdjacencyMode;

import com.tinkerpop.frames.Adjacency;
import com.tinkerpop.frames.Property;
import com.tinkerpop.frames.modules.typedgraph.TypeValue;

/**
 * Creates the TypeScript models which could accommodate the Frames models instances. Also creates a mapping between discriminators (@TypeValue's) and
 * the TS model classes. In TypeScript it's not reliably possible to scan for all models.
 *
 * @author <a href="http://ondra.zizka.cz/">Ondrej Zizka, zizka@seznam.cz</a>
 */
class TypeScriptModelsGenerator
{
    private static final Logger LOG = Logger.getLogger(TypeScriptModelsGenerator.class.getName());

    private static final String DiscriminatorMappingData = "DiscriminatorMappingData";
    private static final String DiscriminatorMapping = "DiscriminatorMapping";
    private static final String BaseModel = "BaseFrameModel";

    /**
     * Path the webapp/ dir which will be used for the imports in the generated models. I.e. <code>import {...} from '$importPathToWebapp';</code>
     * Needs to be the relative path from the final TS models dir to the graph package dir.
     */
    private static final String PATH_TO_GRAPH_PKG = "app/services/graph";
    private static final String TS_SUFFIX = ".ts";
    private static final Pattern pat = Pattern.compile("\\p{javaUpperCase}");
    private TypeScriptModelsGeneratorConfig config;

    public TypeScriptModelsGenerator(TypeScriptModelsGeneratorConfig config)
    {
        this.config = config;
    }

    /**
     * Generates the TypeScript files for models, copying the structure of WindupVertexModel's. The "entry point".
     */
    public void generate(Set<Class<? extends WindupFrame<?>>> modelTypes)
    {
        validateConfig();

        try
        {
            Files.createDirectories(this.config.getOutputPath());
            LOG.info("Creating TypeScript models in " + this.config.getOutputPath().toAbsolutePath());
        }
        catch (IOException ex)
        {
            LOG.severe("Could not create directory for TS models: " + ex.getMessage() + "\n\t" + this.config.getOutputPath());
        }

        Map<String, ModelDescriptor> classesMapping = new TreeMap<>(); // We want deterministic order.

        for (Class<? extends WindupFrame<?>> frameClass : modelTypes)
        {
            if (!(WindupVertexFrame.class.isAssignableFrom(frameClass)))
                continue;

            @SuppressWarnings("unchecked")
            final Class<? extends WindupVertexFrame> frameClass2 = (Class<? extends WindupVertexFrame>) frameClass;
            ModelDescriptor modelDescriptor = createModelDescriptor(frameClass2);
            classesMapping.put(modelDescriptor.discriminator, modelDescriptor);
        }

        addClassesThatAreSkippedForSomeReason(classesMapping);

        for (ModelDescriptor modelDescriptor : classesMapping.values())
        {
            writeTypeScriptModelClass(modelDescriptor, this.config.getAdjacencyMode());
        }

        writeTypeScriptClassesMapping(classesMapping);
        writeTypeScriptBarrel(classesMapping);
    }

    private void validateConfig()
    {
        if (this.config.getImportPathToWebapp() == null)
        {
            LOG.warning("Import path to webapp is null! Setting to ''.");
            this.config.setImportPathToWebapp(Paths.get(""));
        }
    }

    private void addClassesThatAreSkippedForSomeReason(Map<String, ModelDescriptor> classesMapping)
    {
        List<String> artificiallyAddedModels = new ArrayList<>();
        // This is a hack in AmbiguousReferenceMode and WindupVertexListModel
        artificiallyAddedModels.add("WindupVertexFrame");
        // graphTypeManager.getRegisteredTypes() skips the following for some reason.
        artificiallyAddedModels.add("ResourceModel");
        for (String className : artificiallyAddedModels)
        {
            if (!classesMapping.containsKey("ResourceModel"))
            {
                ModelDescriptor md = new ModelDescriptor();
                md.discriminator = "ADummyDiscr_" + className;
                md.modelClassName = className;
                classesMapping.put(md.discriminator, md);
            }
        }
    }

    /**
     * Extracts the information from the given type, based on @Property and @Adjacent annotations.
     */
    private ModelDescriptor createModelDescriptor(Class<? extends WindupVertexFrame> frameClass)
    {
        ModelDescriptor modelDescriptor = new ModelDescriptor();

        // Get the type discriminator string
        TypeValue typeValueAnn = frameClass.getAnnotation(TypeValue.class);
        modelDescriptor.discriminator = typeValueAnn.value();

        modelDescriptor.modelClassName = frameClass.getSimpleName();

        if (frameClass.getInterfaces().length != 1)
            LOG.warning("Model extends more than 1 model. Current TS unmarshaller doesn't support that (yet).");
        modelDescriptor.extendedModels = Arrays.asList(frameClass.getInterfaces()).stream()
                    .filter((x) -> WindupVertexFrame.class.isAssignableFrom(x) && !WindupVertexFrame.class.equals(x))
                    .map(Class::getSimpleName).collect(Collectors.toList());

        // These could be part of the ModelDescriptor.
        BidiMap<String, String> methodNameVsPropName = new DualHashBidiMap<>();
        BidiMap<String, String> methodNameVsEdgeLabel = new DualHashBidiMap<>();

        for (Method method : frameClass.getDeclaredMethods())
        {
            // Get the properties - @Property
            prop:
            {

                Property propAnn = method.getAnnotation(Property.class);
                if (propAnn == null)
                    break prop;

                final Class propertyType = TsGenUtils.getPropertyTypeFromMethod(method);
                if (propertyType == null)
                    break prop;

                final ModelRelation methodInfo = ModelRelation.infoFromMethod(method);
                final String graphPropName = propAnn.value();
                final ModelProperty existing = modelDescriptor.properties.get(graphPropName);

                if (!checkMethodNameVsPropNameConsistency(methodNameVsPropName, methodInfo.beanPropertyName, graphPropName, method,
                            "Property name '%s' of method '%s' doesn't fit previously seen property name '%s' of other method for '%s'."
                                        + "\nCheck the Frames model %s"))
                    // Names don't fit, warning printed.
                    continue;

                // Method base beanPropertyName already seen.
                if (existing != null)
                    continue;

                // This method beanPropertyName was not seen yet.

                final ModelProperty prop = new ModelProperty(methodInfo.beanPropertyName, graphPropName, PrimitiveType.from(propertyType));
                modelDescriptor.properties.put(prop.graphPropertyName, prop);
            }

            // Get the relations - @Adjacent
            adj:
            {

                Adjacency adjAnn = method.getAnnotation(Adjacency.class);
                if (adjAnn == null)
                    break adj;

                // Model class of the other end.
                final Class theOtherType = TsGenUtils.getPropertyTypeFromMethod(method);
                if (theOtherType == null)
                    break adj;

                final ModelRelation methodInfo = ModelRelation.infoFromMethod(method);
                // final boolean alreadySeen = methodNameVsEdgeLabel.containsKey(methodInfo.beanPropertyName);
                final ModelRelation existing = modelDescriptor.relations.get(adjAnn.label());

                if (!checkMethodNameVsPropNameConsistency(methodNameVsEdgeLabel, methodInfo.beanPropertyName, adjAnn.label(), method,
                            "Edge label '%s' of method '%s' doesn't fit previously seen edge label '%s' of other method for '%s'."
                                        + "\nCheck the Frames model %s"))
                    continue;

                // Method base beanPropertyName already seen. Override some traits.
                if (existing != null)
                {
                    existing.isIterable |= methodInfo.isIterable;
                    existing.methodsPresent.addAll(methodInfo.getMethodsPresent());
                    // We want the plural, which is assumably with methods working with Iterable.
                    if (methodInfo.isIterable)
                        existing.beanPropertyName = methodInfo.beanPropertyName;
                    continue;
                }

                ModelType adjType = ModelType.from(theOtherType);
                final ModelRelation modelRelation = new ModelRelation(
                            methodInfo.beanPropertyName,
                            adjAnn.label(),
                            adjAnn.direction().OUT.equals(adjAnn.direction()),
                            adjType,
                            methodInfo.isIterable /// Also for add/remove? (these don't take Iterable)
                );
                modelDescriptor.relations.put(modelRelation.edgeLabel, modelRelation);
            }
        }
        return modelDescriptor;
    }

    /**
     * @param methodNameVsPropName
     * @param methodPropName
     * @param graphPropName
     * @param method
     * @param messageFormat
     *
     * @return false if there was pre-existing mapping of bean property beanPropertyName (getter/setter "base beanPropertyName") to a graph property
     *         beanPropertyName or an edge label, and the examined method beanPropertyName doesn't fit that. true otherwise (No pre-existing mapping
     *         or the names fit.)
     */
    private boolean checkMethodNameVsPropNameConsistency(
                Map<String, String> methodNameVsPropName, final String methodPropName,
                final String graphPropName, Method method, String messageFormat)
    {
        final String existingPropName = methodNameVsPropName.get(methodPropName);
        if (existingPropName != null)
        {
            if (!graphPropName.equals(existingPropName))
            {
                LOG.warning(String.format(messageFormat,
                            graphPropName, method.toString(), existingPropName, methodPropName, method.getDeclaringClass().getName()));
                return false;
            }
        }

        methodNameVsPropName.put(methodPropName, graphPropName);
        return true;
    }

    /**
     * Writes a TypeScript class 'DiscriminatorMapping.ts' with the mapping from the discriminator value (@TypeValue) to TypeScript model class.
     */
    private void writeTypeScriptClassesMapping(Map<String, ModelDescriptor> discriminatorToClassMapping)
    {
        final File mappingFile = this.config.getOutputPath().resolve(DiscriminatorMappingData + TS_SUFFIX).toFile();
        try (FileWriter mappingWriter = new FileWriter(mappingFile))
        {
            Set<String> imported = new HashSet<>();

            final Path path = this.config.getImportPathToWebapp().resolve(PATH_TO_GRAPH_PKG).resolve(BaseModel);
            mappingWriter.write("import {" + BaseModel + "} from '" + path + "';\n");
            imported.add(BaseModel);

            final Path path2 = this.config.getImportPathToWebapp().resolve(PATH_TO_GRAPH_PKG).resolve(DiscriminatorMapping);
            mappingWriter.write("import {" + DiscriminatorMapping + "} from '" + path2 + "';\n\n");
            imported.add(DiscriminatorMapping);

            for (Map.Entry<String, ModelDescriptor> entry : discriminatorToClassMapping.entrySet())
            {
                String importedClass = entry.getValue().modelClassName;
                if (imported.add(importedClass))
                    mappingWriter.write(String.format("import {%1$s} from './%1$s';\n", importedClass));
                else
                    mappingWriter.write(String.format("// {%1$s} wanted to be here again\n", importedClass));
            }

            mappingWriter.write("\n" +
                        "export class " + DiscriminatorMappingData + " extends " + DiscriminatorMapping + "\n{\n" +
                        "    //@Override\n" +
                        "    public static getMapping(): { [key: string]: typeof " + BaseModel + " } {\n" +
                        "        return this.mapping;\n" +
                        "    }\n\n" +

                        "    static mapping: { [key: string]: typeof " + BaseModel + " } = {\n");

            for (Map.Entry<String, ModelDescriptor> entry : discriminatorToClassMapping.entrySet())
            {

                mappingWriter.write("        \"" + entry.getKey() + "\": " + entry.getValue().modelClassName + ",\n");
            }
            mappingWriter.write("    };\n\n");
            mappingWriter.write("    constructor() { super(); };\n");
            mappingWriter.write("};\n");
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Generates a TS barrel, see https://angular.io/docs/ts/latest/glossary.html#B
     *
     * @param discriminatorToClassMapping
     */
    private void writeTypeScriptBarrel(Map<String, ModelDescriptor> discriminatorToClassMapping)
    {
        final File mappingFile = this.config.getOutputPath().resolve("index.ts").toFile();
        try (FileWriter mappingWriter = new FileWriter(mappingFile))
        {
            final Path path = this.config.getImportPathToWebapp().resolve(PATH_TO_GRAPH_PKG).resolve(BaseModel);
            mappingWriter.write("import {" + BaseModel + "} from '" + path + "';\n");
            mappingWriter.write("import {" + DiscriminatorMappingData + "} from './" + DiscriminatorMappingData + "';\n\n");

            for (Map.Entry<String, ModelDescriptor> entry : discriminatorToClassMapping.entrySet())
            {
                mappingWriter.write(String.format("export {%1$s} from './%1$s';\n", entry.getValue().modelClassName));
            }
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Writes a TypeScript class file for the model described by given model descriptor.
     *
     * @param modelDescriptor
     * @param mode The mode in which the models will operate. This can be a passive way (properties and arrays), active way - proxies, or mixed, or
     *            both. Currently implemented is passive.
     */
    private void writeTypeScriptModelClass(ModelDescriptor modelDescriptor, AdjacencyMode mode)
    {
        final File tsFile = this.config.getOutputPath().resolve(formatClassFileName(modelDescriptor.modelClassName, true)).toFile();
        try (FileWriter tsWriter = new FileWriter(tsFile))
        {
            final Path graphPkg = this.config.getImportPathToWebapp().resolve(PATH_TO_GRAPH_PKG);
            tsWriter.write("import {" + BaseModel + "} from '" + graphPkg.resolve(BaseModel) + "';\n");
            tsWriter.write("import {GraphAdjacency} from '" + graphPkg.resolve("graph-adjacency.decorator") + "';\n");
            tsWriter.write("import {GraphProperty} from '" + graphPkg.resolve("graph-property.decorator") + "';\n\n");
            tsWriter.write("import {Observable} from 'rxjs/Observable';\n\n");

            Set<String> imported = new HashSet<>();
            imported.add(BaseModel);

            // Import property and relation types.
            for (ModelRelation relation : modelDescriptor.relations.values())
            {
                final String typeScriptTypeName = relation.type.getTypeScriptTypeName();
                // Don't import this class
                if (typeScriptTypeName.equals(modelDescriptor.modelClassName))
                    continue;
                if (imported.add(typeScriptTypeName))
                    tsWriter.write(String.format("import {%1$s} from './%2$s';\n", typeScriptTypeName, formatClassFileName(typeScriptTypeName, false)));
            }

            List<String> extendedModels = modelDescriptor.extendedModels;
            if (extendedModels == null || extendedModels.isEmpty())
                extendedModels = Collections.singletonList(AdjacencyMode.PROXIED.equals(mode) ? "FrameProxy" : BaseModel);

            // Import extended types.
            tsWriter.write(
                        extendedModels.stream().filter(imported::add)
                                    .map((x) -> {
                                        return String.format("import {%1$s} from './%1$s';\n", x);
                                    })
                                    .collect(Collectors.joining()));

            tsWriter.write("\nexport class " + modelDescriptor.modelClassName + " extends " + String.join(" //", extendedModels) + "\n{\n");
            // tsWriter.write(" private vertexId: number;\n\n");
            tsWriter.write("    static discriminator: string = '" + modelDescriptor.discriminator + "';\n\n");

            // Data for mapping from the graph JSON object to Frame-based models.
            if (!AdjacencyMode.DECORATED.equals(mode))
            {
                tsWriter.write("    static graphPropertyMapping: { [key:string]:string; } = {\n");
                for (ModelProperty property : modelDescriptor.properties.values())
                {
                    tsWriter.write(String.format("        %s: '%s',\n", escapeJSandQuote(property.graphPropertyName), property.beanPropertyName));
                }
                tsWriter.write("    };\n\n");
                tsWriter.write("    static graphRelationMapping: { [key:string]:string; } = {\n");
                for (ModelRelation relation : modelDescriptor.relations.values())
                {
                    try
                    {
                        // edgeLabel: 'propName[TypeValue'
                        // The TypeValue (discriminator) is useful on the client side because the generated JavaScript
                        // has no clue what type is coming or what types should it send back to the server.
                        // While it's coming in the "w:winduptype" value, this may contain several values,
                        // and the models unmarshaller needs to know which one to unmarshall to.
                        tsWriter.write("        " + escapeJSandQuote(relation.edgeLabel) + ": '" + relation.beanPropertyName
                                    + (relation.isIterable ? "[" : "|")
                                    + (relation.type instanceof FrameType ? ((FrameType) relation.type).getFrameDiscriminator() : "") + "',\n");

                    }
                    catch (Exception ex)
                    {
                        String msg = "Error writing relation " + relation.beanPropertyName + ": " + ex.getMessage();
                        tsWriter.write("        // " + msg + "\n");
                        LOG.severe(msg);
                    }
                }
                tsWriter.write("    };\n\n");
            }

            // Actual properties and methods.
            for (ModelProperty property : modelDescriptor.properties.values())
            {
                tsWriter.write(property.toTypeScript(mode));
                tsWriter.write("\n");
            }

            tsWriter.write("\n");

            for (ModelRelation relation : modelDescriptor.relations.values())
            {
                tsWriter.write(relation.toTypeScript(mode));
                tsWriter.write("\n");
            }

            tsWriter.write("}\n");
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, "Failed creating TypeScript model for " + modelDescriptor.toString() + ":\n\t" + ex.getMessage(), ex);
        }
    }

    String escapeJSandQuote(String str)
    {
        return String.format("'%s'", StringEscapeUtils.escapeEcmaScript(str));
    }

    private String formatClassFileName(String className, boolean includeSuffix)
    {
        String suffix = includeSuffix ? TS_SUFFIX : "";

        String separ = ".";
        switch (this.config.getFileNamingStyle())
        {
        case LOWERCASE_DASHES:
            separ = "-";
        case LOWERCASE_DOTS:
        {
            // FIXME - The code below does not actually work (2016/10/28 - jsight)
            // return modelDescriptor.modelClassName.replaceAll(separ, separ) + ".ts";
            Matcher mat = pat.matcher(className);
            return StringUtils.removeStart(mat.replaceAll(separ + "$1"), separ) + suffix;
        }
        default:
        case CAMELCASE:
            return className + suffix;
        }
    }
}

