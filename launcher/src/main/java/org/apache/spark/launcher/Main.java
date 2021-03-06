/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.spark.launcher.CommandBuilderUtils.*;

/**
 * Command line interface for the Spark launcher. Used internally by Spark scripts.
 */
class Main {

  /**
   * Usage: Main [class] [class args]
   * <p>
   * This CLI works in two different modes:
   * <ul>
   *   <li>"spark-submit": if <i>class</i> is "org.apache.spark.deploy.SparkSubmit", the
   *   {@link SparkLauncher} class is used to launch a Spark application.</li>
   *   <li>"spark-class": if another class is provided, an internal Spark class is run.</li>
   * </ul>
   *
   * This class works in tandem with the "bin/spark-class" script on Unix-like systems, and
   * "bin/spark-class2.cmd" batch script on Windows to execute the final command.
   * <p>
   * On Unix-like systems, the output is a list of command arguments, separated by the NULL
   * character. On Windows, the output is a command line suitable for direct execution from the
   * script.
   *
   *
   * 启动master 节点参数：
   *
   *  org.apache.spark.deploy.master.Master --host sysadmindeMacBook-Pro.local --port 7077 --webui-port 8080
   * 启动Work 节点参数：
   *  org.apache.spark.deploy.worker.Worker --webui-port 8081 spark://sysadmindeMacBook-Pro.local:7077
   *
   * 环境变量
   * SPARK_CONF_DIR=/workspace/spark-2.3.2/conf;SPARK_HOME=/workspace/spark-2.3.2
   *
   *
   */
  public static void main(String[] argsArray) throws Exception {
    checkArgument(argsArray.length > 0, "Not enough arguments: missing class name.");

    List<String> args = new ArrayList<>(Arrays.asList(argsArray));

    //todo 获取要执行类的名字
    String className = args.remove(0);

    //是否打印启动命令？
    boolean printLaunchCommand = !isEmpty(System.getenv("SPARK_PRINT_LAUNCH_COMMAND"));
    printLaunchCommand = true ;

    AbstractCommandBuilder builder;

    //TODO 是否提交 SparkSubmit 任务，好吧 SparkSubmit任务也在这里
    if (className.equals("org.apache.spark.deploy.SparkSubmit")) {
      try {




        //      /Library/java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java
        //
        //       
        //
        //      -cp /workspace/spark-2.3.2/conf/:/workspace/spark-2.3.2/assembly/target/scala-2.11/jars/*
        //
        //      -Xmx1g
        //
        //      org.apache.spark.deploy.SparkSubmit
        //
        //      --name spark-test  --class WordCount --master yarn  /A/spark-test.jar /mysqlClean.sql 0
        //


        //todo 在这里构建执行的 shell 脚本
        builder = new SparkSubmitCommandBuilder(args);



      } catch (IllegalArgumentException e) {
        printLaunchCommand = false;
        System.err.println("Error: " + e.getMessage());
        System.err.println();

        MainClassOptionParser parser = new MainClassOptionParser();
        try {
          parser.parse(args);
        } catch (Exception ignored) {
          // Ignore parsing exceptions.
        }

        List<String> help = new ArrayList<>();
        if (parser.className != null) {
          help.add(parser.CLASS);
          help.add(parser.className);
        }
        help.add(parser.USAGE_ERROR);
        builder = new SparkSubmitCommandBuilder(help);
      }
    } else {
      //TODO 其他类在这里构建一个执行对象
      builder = new SparkClassCommandBuilder(className, args);
    }

    Map<String, String> env = new HashMap<>();
    //TODO 构建要执行的命令
    List<String> cmd = builder.buildCommand(env);

    //  启动work 输出的值
    //    0 = "/Library/java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java"
    //    1 = "-cp"
    //    2 = "/workspace/spark-2.3.2/conf/:/workspace/spark-2.3.2/assembly/target/scala-2.11/jars/*"
    //    3 = "-Xmx1g"
    //    4 = "org.apache.spark.deploy.worker.Worker"
    //    5 = "--webui-port"
    //    6 = "8081"
    //    7 = "spark://sysadmindeMacBook-Pro.local:7077"



    //master 集合中的命令
    //  0 = "/Library/java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java"
    //  1 = "-cp"
    //  2 = "/workspace/spark-2.3.2/conf/:/workspace/spark-2.3.2/assembly/target/scala-2.11/jars/*"
    //  3 = "-Xmx1g"
    //  4 = "org.apache.spark.deploy.master.Master"
    //  5 = "--host"
    //  6 = "sysadmindeMacBook-Pro.local"
    //  7 = "--port"
    //  8 = "7077"
    //  9 = "--webui-port"
    //  10 = "8080"


    if (printLaunchCommand) {
      System.err.println("Spark Command: " + join(" ", cmd));
      System.err.println("========================================");
    }


    if (isWindows()) {
      //是否为Windows的
      System.out.println(prepareWindowsCommand(cmd, env));
    } else {
      // In bash, use NULL as the arg separator since it cannot be used in an argument.
      //todo  将集合中的命令进行拼装输出，由shell脚本获取需要执行的java脚本
      //启动 work 输出的值


      List<String> bashCmd = prepareBashCommand(cmd, env);
      for (String c : bashCmd) {
        System.out.print(c);
        System.out.print('\0');
      }


      //启动 worker 输出的值
      //  /Library/java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java 
      //  -cp /workspace/spark-2.3.2/conf/:/workspace/spark-2.3.2/assembly/target/scala-2.11/jars/* 
      //  -Xmx1g 
      //  org.apache.spark.deploy.worker.Worker 
      //  --webui-port 8081 spark://sysadmindeMacBook-Pro.local:7077 
      //



      //启动 master 输出的值
      // /Library/java/JavaVirtualMachines/jdk1.8.0_161.jdk/Contents/Home/bin/java -cp
      // /workspace/spark-2.3.2/conf/:/workspace/spark-2.3.2/assembly/target/scala-2.11/jars/*
      // -Xmx1g
      // org.apache.spark.deploy.master.Master
      // --host sysadmindeMacBook-Pro.local --port 7077 --webui-port 8080






    }
  }

  /**
   * Prepare a command line for execution from a Windows batch script.
   *
   * The method quotes all arguments so that spaces are handled as expected. Quotes within arguments
   * are "double quoted" (which is batch for escaping a quote). This page has more details about
   * quoting and other batch script fun stuff: http://ss64.com/nt/syntax-esc.html
   */
  private static String prepareWindowsCommand(List<String> cmd, Map<String, String> childEnv) {
    StringBuilder cmdline = new StringBuilder();
    for (Map.Entry<String, String> e : childEnv.entrySet()) {
      cmdline.append(String.format("set %s=%s", e.getKey(), e.getValue()));
      cmdline.append(" && ");
    }
    for (String arg : cmd) {
      cmdline.append(quoteForBatchScript(arg));
      cmdline.append(" ");
    }
    return cmdline.toString();
  }

  /**
   * Prepare the command for execution from a bash script. The final command will have commands to
   * set up any needed environment variables needed by the child process.
   */
  private static List<String> prepareBashCommand(List<String> cmd, Map<String, String> childEnv) {
    if (childEnv.isEmpty()) {
      return cmd;
    }

    List<String> newCmd = new ArrayList<>();
    newCmd.add("env");

    for (Map.Entry<String, String> e : childEnv.entrySet()) {
      newCmd.add(String.format("%s=%s", e.getKey(), e.getValue()));
    }
    newCmd.addAll(cmd);
    return newCmd;
  }

  /**
   * A parser used when command line parsing fails for spark-submit. It's used as a best-effort
   * at trying to identify the class the user wanted to invoke, since that may require special
   * usage strings (handled by SparkSubmitArguments).
   */
  private static class MainClassOptionParser extends SparkSubmitOptionParser {

    String className;

    @Override
    protected boolean handle(String opt, String value) {
      if (CLASS.equals(opt)) {
        className = value;
      }
      return false;
    }

    @Override
    protected boolean handleUnknown(String opt) {
      return false;
    }

    @Override
    protected void handleExtraArgs(List<String> extra) {

    }

  }

}
