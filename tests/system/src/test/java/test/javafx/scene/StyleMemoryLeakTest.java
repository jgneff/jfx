/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.javafx.scene;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import test.util.Util;
import test.util.memory.JMemoryBuddy;


public class StyleMemoryLeakTest {

    @BeforeAll
    public static void initFX() throws Exception {
        CountDownLatch startupLatch = new CountDownLatch(1);
        Platform.setImplicitExit(false);

        Util.startup(startupLatch, () -> {
            startupLatch.countDown();
        });
    }

    @AfterAll
    public static void teardownOnce() {
        Util.shutdown();
    }

    @Test
    public void testRootNodeMemoryLeak() throws Exception {
        JMemoryBuddy.memoryTest((checker) -> {
            CountDownLatch showingLatch = new CountDownLatch(1);

            Button toBeRemoved = new Button();
            Group root = new Group();
            AtomicReference<Stage> stage = new AtomicReference<>();

            Util.runAndWait(() -> {
                stage.set(new Stage());
                stage.get().setOnShown(l -> {
                    Platform.runLater(() -> showingLatch.countDown());
                });
                stage.get().setScene(new Scene(root));
                stage.get().show();
            });

            try {
                assertTrue(showingLatch.await(15, TimeUnit.SECONDS), "Timeout waiting test stage");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Util.runAndWait(() -> {
                root.getChildren().clear();
                stage.get().hide();
            });

            checker.assertCollectable(stage.get());
            checker.setAsReferenced(toBeRemoved);
        });
    }
}
