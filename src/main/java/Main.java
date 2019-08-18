import helper.Configuration;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.FileNotFoundException;

/**
 * Created by Carol on 2018/11/27.
 */

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build()
                .defaultHelp(true)
                .description("Analyse a git project and build visible graph");

        parser.addArgument("-p", "--project")
                .required(false)
                .help("git project root directory");
        parser.addArgument("-f", "--filter")
                .help("optional, type filter switch for source file");
        parser.addArgument("-d", "--database")
                .help("optional, specify database path");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        //String project = ns.getString("project");
        String project = "./platform_frameworks_base";
        Configuration.project = project;

        String filter = ns.getString("filter");
        if (filter == null){
            filter = "";
        }

        String database = ns.getString("database");
        System.out.println(database);
        if (database != null){
            Configuration.database = database;
        }
        Configuration.database = "/Users/vlpha7/Library/Application Support/Neo4j Desktop/Application/neo4jDatabases/database-8c567e3a-0127-46e5-ab39-86011226e206/installation-3.5.6/data/databases/graph.db";
        System.out.println("Starting...");
        new Analyser(project, filter).run();
        System.out.println("Done.");
    }
}
