package com.modernjava.virtualthreads;


import com.modernjava.util.CommonUtil;

import static com.modernjava.util.LoggerUtil.log;

public class ExploreVirtualThreads {

    public static void doSomeWork() {
        log("started doSomeWork");
        CommonUtil.sleep(1000);
        log("finished doSomeWork");
    }

    public static void main(String[] args) {

        var thread1 = Thread.ofVirtual().name("t1");
        var thread2 = Thread.ofVirtual().name("t2");
        var thread3 = Thread.ofVirtual().name("t3")
                .unstarted(()->{
                    log("Run task 3 in the background");
                });

        thread1.start(()->{
            log("Run task 1 in background");
        });

        thread2.start(ExploreVirtualThreads::doSomeWork);
        thread3.start();

        log("Program Completed");

        CommonUtil.sleep(2000);


    }
}
