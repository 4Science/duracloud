/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.retrieval.config;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading the configuration parameters for the Retrieval Tool
 *
 * @author: Bill Branan
 * Date: Oct 12, 2010
 */
public class RetrievalToolConfigParser {

    private final Logger logger =
        LoggerFactory.getLogger(RetrievalToolConfigParser.class);

    protected static final int DEFAULT_PORT = 443;
    protected static final int DEFAULT_NUM_THREADS = 3;
    protected static final String DEFAULT_CONTEXT = "durastore";

    private Options cmdOptions;

    /**
     * Creates a parser for command line configuration options.
     */
    public RetrievalToolConfigParser() {
       // Command Line Options
       cmdOptions = new Options();

       Option hostOption =
           new Option("h", "host", true,
                      "the host address of the DuraCloud " +
                      "DuraStore application");
       hostOption.setRequired(true);
       cmdOptions.addOption(hostOption);

       Option portOption =
           new Option("r", "port", true,
                      "the port of the DuraCloud DuraStore application " +
                      "(optional, default value is " + DEFAULT_PORT + ")");
       portOption.setRequired(false);
       cmdOptions.addOption(portOption);

       Option usernameOption =
           new Option("u", "username", true,
                      "the username necessary to perform writes to DuraStore");
       usernameOption.setRequired(true);
       cmdOptions.addOption(usernameOption);

       Option passwordOption =
           new Option("p", "password", true,
                      "the password necessary to perform writes to DuraStore");
       passwordOption.setRequired(true);
       cmdOptions.addOption(passwordOption);

       Option spaces =
           new Option("s", "spaces", true,
                      "the space or spaces from which content will be " +
                      "retrieved");
       spaces.setRequired(false);
       spaces.setArgs(Option.UNLIMITED_VALUES);
       cmdOptions.addOption(spaces);

       Option allSpaces =
           new Option("a", "all-spaces", false,
                      "indicates that all spaces should be retrieved; if " +
                      "this option is included the -s option is ignored " +
                      "(optional, not set by default)");
        allSpaces.setRequired(false);
        cmdOptions.addOption(allSpaces);

       Option contentDirOption =
           new Option("c", "content-dir", true,
                      "retrieved content is stored in this local directory");
       contentDirOption.setRequired(true);
       cmdOptions.addOption(contentDirOption);

       Option workDirOption =
           new Option("w", "work-dir", true,
                      "logs and output files will be stored in the work " +
                      "directory");
       workDirOption.setRequired(true);
       cmdOptions.addOption(workDirOption);

       Option overwrite =
           new Option("o", "overwrite", false,
                      "indicates that existing local files which differ " +
                      "from files in DuraCloud under the same path and name " +
                      "sould be overwritten rather than copied " +
                      "(optional, not set by default)");
        overwrite.setRequired(false);
        cmdOptions.addOption(overwrite);

       Option numThreads =
           new Option("t", "threads", true,
                      "the number of threads in the pool used to manage " +
                      "file transfers (optional, default value is " +
                      DEFAULT_NUM_THREADS + ")");
        numThreads.setRequired(false);
        cmdOptions.addOption(numThreads);
    }

    /**
     * Parses command line configuration into an object structure, validates
     * correct values along the way.
     *
     * Prints a help message and exits the JVM on parse failure.
     *
     * @param args command line configuration values
     * @return populated RetrievalToolConfig
     */
    public RetrievalToolConfig processCommandLine(String[] args) {
        RetrievalToolConfig config = null;
        try {
            config = processOptions(args);
        } catch (ParseException e) {
            printHelp(e.getMessage());
        }
        return config;
    }

    protected RetrievalToolConfig processOptions(String[] args)
        throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(cmdOptions, args);
        RetrievalToolConfig config = new RetrievalToolConfig();

        config.setContext(DEFAULT_CONTEXT);
        config.setHost(cmd.getOptionValue("h"));
        config.setUsername(cmd.getOptionValue("u"));
        config.setPassword(cmd.getOptionValue("p"));

        if(cmd.hasOption("r")) {
            try {
                config.setPort(Integer.valueOf(cmd.getOptionValue("r")));
            } catch(NumberFormatException e) {
                throw new ParseException("The value for port (-r) must be " +
                                         "a number.");
            }
        } else {
            config.setPort(DEFAULT_PORT);
        }

        if(!cmd.hasOption("s") && !cmd.hasOption("a")) {
            throw new ParseException("Either a list of spaces (-s) should be " +
                "provided or the all spaces flag (-a) must be set.");
        }

        if(cmd.hasOption("s")) {
            String[] spaces = cmd.getOptionValues("s");
            List<String> spacesList = new ArrayList<String>();
            for(String space : spaces) {
                if(space != null && !space.equals("")) {
                    spacesList.add(space);
                }
            }
            config.setSpaces(spacesList);
        }

        if(cmd.hasOption("a")) {
            config.setAllSpaces(true);
        } else {
            config.setAllSpaces(false);
        }        

        File contentDir = new File(cmd.getOptionValue("c"));
        if(contentDir.exists()) {
            if(!contentDir.isDirectory()) {
                throw new ParseException("Content Dir paramter must provide " +
                                         "the path to a directory.");
            }
        } else {
            contentDir.mkdirs();
        }
        contentDir.setWritable(true);
        config.setContentDir(contentDir);

        File workDir = new File(cmd.getOptionValue("w"));
        if(workDir.exists()) {
            if(!workDir.isDirectory()) {
                throw new ParseException("Wirk Dir paramter must provide " +
                                         "the path to a directory.");
            }
        } else {
            workDir.mkdirs();
        }
        workDir.setWritable(true);
        config.setWorkDir(workDir);

        if(cmd.hasOption("o")) {
            config.setOverwrite(true);
        } else {
            config.setOverwrite(false);
        }

        if(cmd.hasOption("t")) {
            try {
                config.setNumThreads(Integer.valueOf(cmd.getOptionValue("t")));
            } catch(NumberFormatException e) {
                throw new ParseException("The value for threads (-t) must " +
                                         "be a number.");
            }
        } else {
            config.setNumThreads(DEFAULT_NUM_THREADS);
        }

        return config;
    }

    private void printHelp(String message) {
        System.out.println("\n-----------------------\n" +
                           message +
                           "\n-----------------------\n");

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Running Retrieval Tool",
                            cmdOptions);
        System.exit(1);
    }

}
