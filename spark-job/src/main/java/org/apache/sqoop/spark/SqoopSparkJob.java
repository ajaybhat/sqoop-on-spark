package org.apache.sqoop.spark;

import org.apache.commons.cli.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.sqoop.common.Direction;
import org.apache.sqoop.core.ConfigurationConstants;
import org.apache.sqoop.core.SqoopServer;
import org.apache.sqoop.driver.JobManager;
import org.apache.sqoop.driver.JobRequest;
import org.apache.sqoop.job.spark.SparkDestroyerUtil;
import org.apache.sqoop.model.MJob;
import org.apache.sqoop.model.MSubmission;
import org.apache.sqoop.request.HttpEventContext;
import org.apache.sqoop.submission.spark.SqoopSparkDriver;

import java.io.Serializable;
import java.util.ListIterator;

public class SqoopSparkJob implements Serializable {

  private MJob job;
  private static CommandLineParser parser;

  static class SqoopGnuParser extends GnuParser {

    private final boolean ignoreUnrecognizedOption;

    public SqoopGnuParser(final boolean ignoreUnrecognizedOption) {
      this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void processOption(final String arg, final ListIterator iter) throws ParseException {
      boolean hasOption = getOptions().hasOption(arg);

      // this allows us to parse the options for command and then parse again
      // based on command
      if (hasOption || !ignoreUnrecognizedOption) {
        super.processOption(arg, iter);
      }
    }

  }

  SqoopSparkJob() {
    parser = new SqoopGnuParser(true);

  }

  public void setJob(MJob job) {
    this.job = job;
  }

  public static CommandLine parseArgs(Options options, String[] args) {

    CommandLine commandLineArgs;
    try {
      // parse the command line arguments
      commandLineArgs = parser.parse(options, args, false);
    } catch (ParseException pe) {
      throw new RuntimeException("Parsing failed for command option:", pe);
    }
    return commandLineArgs;
  }

  @SuppressWarnings("static-access")
  public static void addCommonOptions(Options options) {


    options.addOption(OptionBuilder.withLongOpt("numL").withDescription("loader parallelism")
        .hasArg().withArgName("numLoaders").create());

    options.addOption(OptionBuilder.withLongOpt("numE").withDescription("extractor parallelism")
        .hasArg().withArgName("numExtractors").create());

    options.addOption(OptionBuilder.withLongOpt("defaultExtractors").withDescription("default extractor parallelism")
        .hasArg().withArgName("defaultExtractors").create());

    options.addOption(OptionBuilder.withLongOpt("confDir").withDescription("config dir for sqoop")
        .hasArg().isRequired().withArgName("confDir").create());
  }

  public SparkConf init(CommandLine cArgs) throws ClassNotFoundException {
    System.setProperty(ConfigurationConstants.SYSPROP_CONFIG_DIR, cArgs.getOptionValue("confDir"));
    // by default it is local, override based on the submit parameter
    SparkConf conf = new SparkConf().setAppName("sqoop-spark").setMaster("local");
    if (cArgs.getOptionValue("defaultExtractors") != null) {
      conf.set(SqoopSparkDriver.DEFAULT_EXTRACTORS, cArgs.getOptionValue("defaultExtractors"));
    }
    if (cArgs.getOptionValue("numL") != null) {
      conf.set(SqoopSparkDriver.NUM_LOADERS, cArgs.getOptionValue("numL"));
    }
    // hack to load extra classes directly
    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    Class.forName("com.mysql.jdbc.Driver");
    SqoopServer.initialize();
    return conf;
  }

  public void execute(SparkConf conf, JavaSparkContext context) throws Exception {

    if (job == null) {
      throw new RuntimeException("Job not set for spark execution");
    }
    HttpEventContext ctx = new HttpEventContext();
    // TODO: use standard username
    ctx.setUsername("spark-sqoop");
    MSubmission mSubmission = JobManager.getInstance().createJobSubmission(ctx,
        job.getPersistenceId());
    JobRequest jobRequest = JobManager.getInstance().createJobRequest(job.getPersistenceId(),
        mSubmission);
    JobManager.getInstance().prepareJob(jobRequest);
    SqoopSparkDriver.execute(jobRequest, conf, context);
    SparkDestroyerUtil.executeDestroyer(true, jobRequest, Direction.FROM);
    SparkDestroyerUtil.executeDestroyer(true, jobRequest, Direction.TO);
    SqoopServer.destroy();
  }
}
