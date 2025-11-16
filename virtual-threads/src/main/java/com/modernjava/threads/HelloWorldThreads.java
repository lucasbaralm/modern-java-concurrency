package com.modernjava.threads;


import static com.modernjava.util.CommonUtil.sleep;
import static com.modernjava.util.LoggerUtil.log;

public class HelloWorldThreads {
    private static String result="";

    private static void hello(){
        sleep(500);
        result = result.concat("Hello");

    }
    private static void world(){
        sleep(600);
        result = result.concat("World");
    }

    public static void main(String[] args) throws InterruptedException {

        var thread1 = Thread.ofPlatform().name("t1")
                .start(HelloWorldThreads::hello);
        var thread2 = Thread.ofPlatform().name("t2")
                .start(HelloWorldThreads::world);

        thread1.join();
        thread2.join();

        log(result);
    }
}
