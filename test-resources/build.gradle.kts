/*
 * Copyright © 2017-2020 WireGuard LLC.
 * Copyright © 2018-2020 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
configurations {
    register("testRes")
}

tasks.register<Jar>("testJar") {
    classifier = "testRes"
    from("src/test/resources")
}

artifacts {
    add("testRes", tasks.getByName("testJar"))
}
