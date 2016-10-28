export class TestGraphData
{
    static BASE_WINDUP_CONFIG = {
        "_id": 256,
        "_mode": "vertex",
        "w:winduptype": ["BaseWindupConfiguration"],
        "fetchRemoteResources": false,
        "csv": false,
        "keepWorkDirs": true,
        "vertices_out": {
            "edgeLabel": {
                "direction": "OUT", //|'IN'
                "_type": "link",
                "link": "http://localhost/rest/graph/by-id/256" // A link to a set of vertices
            },
        },
        "otherEdgeLabel": { }
    };

    static TEST_GRAPH_MODEL_DATA = {
        "_id": 456,
        "_mode": "vertex",
        "w:winduptype": ["TestGenerator"],

        "bar": "bar123", "name": "Blake Ross", "rank": "capitain",

        "vertices_out": {
            // -> ship
            "commands": {
                "vertices": [
                    { "_id": 123, "w:winduptype": ["TestShip"], "name": "USS Firefox"},
                ]
            },
            // -> colonizedPlanet
            "colonizes": {
                "vertices": [
                    { "_id": 213, "w:winduptype": ["TestPlanet"],  "name": "Mars" },
                    { "_id": 214, "w:winduptype": ["TestPlanet"],  "name": "Venus" },
                    //{ "_mode": "link", "link": "by-id=215" },
                ]
            },
            "shuttles": {
                "direction": "OUT", //|'IN'
                "_type": "link",
                "link": "http://localhost/rest/graph/by-id/456" // A link to a set of vertices
            },
            "fighter": {
                "direction": "OUT", //|'IN'
                "_type": "link",
                "link": "http://localhost/rest/graph/by-id/456" // A link to a set of vertices
            },
        },
    };


    static TEST_FILE_MODELS = [
        {
            "w:winduptype": [
                "FileResource"
            ],
            "fileName": "nonxa",
            "vertices_in": {
                "parentFile": {
                    "_type": "link",
                    "link": "http://localhost:8080/windup-web-services/rest/graph/2854/edges/16384/IN/parentFile",
                    "direction": "IN"
                }
            },
            "filePath": "/path/to/nonxa",
            "windupGenerated": false,
            "_type": "vertex",
            "vertices_out": {
                "parentFile": {
                    "_type": "link",
                    "link": "http://localhost:8080/windup-web-services/rest/graph/2854/edges/16384/OUT/parentFile",
                    "direction": "OUT"
                }
            },
            "_id": 16384,
            "isDirectory": true
        },
        {
            "fileName": "NonXAResource.class",
            "vertices_in": {
                "projectModelToFile": {
                    "_type": "link",
                    "link": "http://localhost:8080/windup-web-services/rest/graph/2854/edges/16640/IN/projectModelToFile",
                    "direction": "IN"
                }
            },
            "filePath": "/path/to/nonxa/NonXAResource.class",
            "_type": "vertex",
            "skipDecompilation": true,
            "majorVersion": 49,
            "w:winduptype": [
                "FileResource",
                "JavaClassFileModel"
            ],
            "windupGenerated": false,
            "vertices_out": {
                "parentFile": {
                    "_type": "link",
                    "link": "http://localhost:8080/windup-web-services/rest/graph/2854/edges/16640/OUT/parentFile",
                    "direction": "OUT"
                }
            },
            "packageName": "weblogic.transaction.nonxa",
            "_id": 16640,
            "minorVersion": 0,
            "isDirectory": false
        }
    ];
}