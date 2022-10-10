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
[Jipl](https://github.com/HoloInJava/jipl) is a **free**, **open-source** and **modular** programming language, designed for **simplicity** and **expansibility**. <br>
If your user has to write some code in your Java software, it can be a challenge to make it work properly, and that is *exactly* why we created this project. <br>
Being as **beginner-friendly** as possible with its **easy syntax**, Jipl offers a [large toolkit](https://github.com/HoloInJava/jipl) of built-in functions while remaining as concise as possible.
## Projects using Jipl
 - [Coding in Minecraft](https://github.com/HoloInJava/Code-in-Minecraft), a plugin that lets you write code within the game.
 - A soon coming full game engine in Java, using Jipl as its custom programming language.

# Quickstart
After downloading the [`JIPL.jar`](https://github.com/HoloInJava/jipl/raw/master/JIPL.jar) file provided, there is a handful of ways you can use it.
## Implement it in your project
You can add the given jar file in your **project**, you will then have access to the entirety of the JIPL project from the Java perspective. <br>
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
Or, in a more practical way, you can add file names as arguments to the command, like `java -jar JIPL.jar test1.jipl test2.jipl`; this executes the local files `test1.jipl` then `test2.jipl` using JIPL.

# A Quick Documentation :page_with_curl:
This section will quickly go trough every feature currently available in JIPL, further examples can be found in the [`/examples`](https://github.com/HoloInJava/jipl/tree/master/examples) folder. Also, please feel free to suggest any new example from your creation, we`ll gladly add it!

## Variables
To declare a new variable in the current scope, we use the keyword `var`, even though it is not mandatory, it ensures that the variable you are declaring is within the scope and that you are not modifying a pre-existing variable. <br>
There is five fundamental variables types in Jipl.
- Numbers
- [Strings](#strings)
- [Lists](#lists)
- [Functions](#functions)
- [Objects](#objects)
Here is a quick example of each one :
```python
var age = 18;
var name = "Holo";
var projects = ["Jipl", "Code in Minecraft", "JGE"];
var addition = function(a, b): a+b;
var file = new File("cool_file.txt");

var yearOfBirth = 2022 - age;
var fullName = name + "InJava";
projects.add("more things coming soon");
print(addition(10, 3));
print(file.exists());
```

### Syntactic sugar
To add a bit of sweetness into your code and make it more concise, we've added standard syntactic sugar.
Here is a quick example :
```python
var price = 19.99;
price*=0.95; # Simulating a 5% discount, equivalent to  price = price * 0.95

var names = ["John", "James", "Robert"];
names+="Mary"; # Adding Mary to the list of names, equivalent to  names.add("Mary")

var playerCount = 456;
playerCount/=2; # Dividing the player count by 2, equivalent to  playerCount = playerCount / 2;
```

## Control Structures
### If, else and elseif
```python
var age = 18;

if age >= 18: print("You are major.");
else: print("You are minor.");

```
