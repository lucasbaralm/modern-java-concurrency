package com.modernjava.threads;


import com.modernjava.util.CommonUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static com.modernjava.util.LoggerUtil.log;

public class MaxThreads {

    static AtomicInteger atomicInteger = new AtomicInteger();

    public static void doSomeWork(int index) {
        log("started doSomeWork : " + index);
        //Any task that's started by a thread is blocked until it completes.
        //It could be any IO call such as HTTP or File IO call.
        CommonUtil.sleep(5000);
        log("finished doSomeWork : " + index);
    }

    public static void main(String[] args) {

        int MAX_THREADS = 1000;
        IntStream.rangeClosed(1, MAX_THREADS).forEach(i -> {
            Thread.ofPlatform().start(() -> doSomeWork(i));
        });
        log("Program Completed!");
        //Platform threads are tied to OS threads. Each platform thread takes to 1MB to 2MB of memory.
        //So, creating too many platform threads can lead to OutOfMemoryError if heap memory is exceeded.
        //Other drawback include the blocking nature of java platform threads. When you await for responses of threads
        //Tomcat thread poll exausted for example, caused by too many platform threads or service unavailability or lag
        //Solution: Virtual threads way more lightweight and efficient. we create a vt per request
    }
}
