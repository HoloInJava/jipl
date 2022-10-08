# Java Interpreted Programming Language: JIPL

<p align="center">
  <a href="https://github.com/HoloInJava/jipl">
    <img src="https://user-images.githubusercontent.com/77677018/194699224-4443d04d-8743-40c4-9c7d-641fff2f7c50.png" width="256"> 
  </a>
</p>
<p align="center">
  <i align="center">
    The <b>easiest</b> programming language in the world! :blue_heart:
  </i>
</p>

# Overview
[JIPL](https://github.com/HoloInJava/jipl) is a **free**, **open-source** and **modular** programming language, designed for **simplicity** and **expansibility**. <br>
If your user has to write some code in your Java software, it can be a challenge to make it work properly, and that is *exactly* why we created this project. <br>
Being as **beginner-friendly** as possible with its **easy syntax**, JIPL offers a [large toolkit](https://github.com/HoloInJava/jipl) of built-in functions while remaining as concise as possible.

# Quickstart
After downloading the [`JIPL.jar`](https://github.com/HoloInJava/jipl/blob/master/JIPL.jar) file provided, there is a handful of ways you can use it.
## Implement it in your project
You can add the given jar file in your project, you will then have access to the entirety of the JIPL project from the Java perspective. <br>
Here is how you can add the jar file to your project using [Eclipse](https://stackoverflow.com/questions/3280353/how-to-import-a-jar-in-eclipse), or [IntelliJ IDEA](https://www.geeksforgeeks.org/how-to-add-external-jar-file-to-an-intellij-idea-project/). <br>

The following block is an example where the file "test.jipl" is executed. 
```Java
public class Main {
  public static void main(String[] args) {
    JIPL.run("test.jipl", JIPL.getGlobalContext());
  }
}
```

## Using it in the console
You can run the given jar file directly from the console using `java -jar JIPL.jar`, you will then be able to input any JIPL expression and get the output.
Or, in a more practical way, you can add a file name as an argument to the command, like `java -jar JIPL.jar test.jipl`; this executes the local file "test.jipl" using JIPL.

# A Quick Documentation :page_with_curl:
This section will quickly go trough every feature currently available in JIPL, further examples can be found in the `/src/examples` folder.

## Variables
