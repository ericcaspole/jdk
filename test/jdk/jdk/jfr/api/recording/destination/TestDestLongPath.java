/*
 * Copyright (c) 2018, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.jfr.api.recording.destination;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.FileHelper;

/**
 * @test
 * @summary Set destination to a long path
 * @requires vm.flagless
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.recording.destination.TestDestLongPath
 */
public class TestDestLongPath {

    public static void main(String[] args) throws Throwable {
        Path dir = FileHelper.createLongDir(Paths.get("."));
        Path dest = Paths.get(dir.toString(), "my.jfr");
        Recording r = new Recording();
        r.enable(EventNames.OSInformation);
        r.setToDisk(true);
        r.setDestination(dest);
        r.start();
        r.stop();
        r.close();
        Asserts.assertTrue(Files.exists(dest), "No recording file: " + dest);
        List<RecordedEvent> events = RecordingFile.readAllEvents(dest);
        Asserts.assertFalse(events.isEmpty(), "No event found");
        System.out.printf("Found event %s%n", events.getFirst().getEventType().getName());
    }

}
