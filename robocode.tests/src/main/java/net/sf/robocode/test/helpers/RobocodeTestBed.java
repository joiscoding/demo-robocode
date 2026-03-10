/*
 * Copyright (c) 2001-2025 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://robocode.sourceforge.io/license/epl-v10.html
 */
package net.sf.robocode.test.helpers;


import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import robocode.control.RobotTestBed;

import java.io.File;

/**
 * @author Pavel Savara (original)
 */
public abstract class RobocodeTestBed extends RobotTestBed {

    protected boolean isJdk25Compatible() {
        return false;
    }

    private int getJavaMajorVersion() {
        String version = System.getProperty("java.version");

        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, version.lastIndexOf('.')));
        }

        int index = version.indexOf('.');
        if (index < 0) {
            index = version.indexOf('-');
        }
        if (index > 0) {
            version = version.substring(0, index);
        }
        return Integer.parseInt(version);
    }

    @Override
    protected void beforeInit() {
        if (!new File("").getAbsolutePath().endsWith("robocode.tests")) {
            throw new Error("Please run test with current directory in 'robocode.tests'");
        }
        super.beforeInit();
    }

    @Before
    public void before() {
        Assume.assumeTrue(
            "This robot integration test is not compatible with JDK 25+",
            isJdk25Compatible() || getJavaMajorVersion() < 25
        );
        super.before();
    }

    @After
    public void after() {
        super.after();
    }
}
