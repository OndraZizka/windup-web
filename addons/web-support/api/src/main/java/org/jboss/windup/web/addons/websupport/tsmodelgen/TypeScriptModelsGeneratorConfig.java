package org.jboss.windup.web.addons.websupport.tsmodelgen;

import java.nio.file.Path;

/**
 * Defines how the TS files will be generated.
 *
 * @author <a href="mailto:zizka@seznam.cz">Ondrej Zizka</a>
 */
public class TypeScriptModelsGeneratorConfig
{
    private Path outputPath;
    private Path importPathToWebapp;
    private AdjacencyMode adjacencyMode;
    private FileNamingStyle fileNamingStyle;

    /**
     * Location of the generated report files.
     */
    public Path getOutputPath()
    {
        return outputPath;
    }

    /**
     * Location of the generated report files.
     */
    public void setOutputPath(Path outputPath)
    {
        this.outputPath = outputPath;
    }

    /**
     * Path the webapp/ dir which will be used for the generated TS models imports. E.g. <code>import {...} from '$importPathToWebapp';</code>
     */
    public Path getImportPathToWebapp()
    {
        return importPathToWebapp;
    }

    /**
     * Path the webapp/ dir which will be used for the generated TS models imports. E.g. <code>import {...} from '$importPathToWebapp';</code>
     */
    public void setImportPathToWebapp(Path importPathToWebapp)
    {
        this.importPathToWebapp = importPathToWebapp;
    }

    /**
     * How adjacency is handled - plain arrays ('MATERIALIZED') or proxied methods ('PROXIED').
     */
    public AdjacencyMode getAdjacencyMode()
    {
        return adjacencyMode;
    }

    /**
     * How adjacency is handled - plain arrays ('MATERIALIZED') or proxied methods ('PROXIED').
     */
    public void setAdjacencyMode(AdjacencyMode adjacencyMode)
    {
        this.adjacencyMode = adjacencyMode;
    }

    /**
     * FooModel.ts vs foo.model.ts vs foo-model.ts.
     */
    public FileNamingStyle getFileNamingStyle()
    {
        return fileNamingStyle;
    }

    /**
     * FooModel.ts vs foo.model.ts vs foo-model.ts.
     */
    public void setFileNamingStyle(FileNamingStyle fileNamingStyle)
    {
        this.fileNamingStyle = fileNamingStyle;
    }

    public enum AdjacencyMode
    {
        PROXIED, MATERIALIZED, MIXED, DECORATED;
    }

    public enum FileNamingStyle
    {
        CAMELCASE, LOWERCASE_DASHES, LOWERCASE_DOTS;
    }

}
