package org.apache.sqoop.spark;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * Created by ajayb on 03/02/16.
 */
public class SqoopSparkOptionsUtil {
    @SuppressWarnings("static-access")
   public static Options createOptions() {

        Options options = new Options();

        options.addOption(OptionBuilder.withLongOpt("jdbcString")
                .withDescription("jdbc connection string").hasArg().isRequired()
                .withArgName("jdbcConnectionString").create());

        options.addOption(OptionBuilder.withLongOpt("u").withDescription("jdbc username").hasArg()
                .isRequired().withArgName("username").create());

        options.addOption(OptionBuilder.withLongOpt("p").withDescription("jdbc password").hasArg()
                .withArgName("password").create());

        options.addOption(OptionBuilder.withLongOpt("table").withDescription("jdbc table").hasArg()
                .isRequired().withArgName("table").create());

        options.addOption(OptionBuilder.withLongOpt("partitionCol")
                .withDescription("jdbc table parition column").hasArg().withArgName("pc").create());

        options.addOption(OptionBuilder.withLongOpt("outputDir").withDescription("hdfs output dir")
                .hasArg().isRequired().withArgName("outputDir").create());

        SqoopSparkJob.addCommonOptions(options);

        return options;
    }
}
