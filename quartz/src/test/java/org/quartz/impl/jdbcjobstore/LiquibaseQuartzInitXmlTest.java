/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.impl.jdbcjobstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Guards the structure of {@code liquibase.quartz.init.xml} against the
 * {@code sql_generate_invisible_primary_key} regression (issue #1489).
 *
 * <p>When MySQL 8.0.30+ runs with GIPK enabled (the default on Azure MySQL
 * Flexible Server and not disableable via SQL), it auto-creates an invisible
 * primary key on any table created without an explicit primary key. Declaring
 * the composite primary keys inline in {@code <createTable>} makes MySQL see
 * the explicit key at creation time, so no generated key collides with a later
 * {@code <addPrimaryKey>} (error 1068 "Multiple primary key defined").
 */
class LiquibaseQuartzInitXmlTest {

    private static final String CHANGELOG =
            "/org/quartz/impl/jdbcjobstore/liquibase.quartz.init.xml";

    private static final String ANY_NS = "*";

    @Test
    void testPrimaryKeysAreInlinedInCreateTable() throws Exception {
        Document changeLog = loadChangeLog();

        // No standalone <addPrimaryKey>: it would collide with a GIPK-generated
        // invisible key on a table created without an inline primary key.
        assertEquals(
                0,
                changeLog.getElementsByTagNameNS(ANY_NS, "addPrimaryKey").getLength(),
                "primary keys must be declared inline in createTable, not via addPrimaryKey");

        // Every created table must carry an inline primary key so MySQL never
        // auto-generates one under sql_generate_invisible_primary_key=ON.
        NodeList tables = changeLog.getElementsByTagNameNS(ANY_NS, "createTable");
        assertFalse(tables.getLength() == 0, "expected at least one createTable change");
        for (int i = 0; i < tables.getLength(); i++) {
            Element table = (Element) tables.item(i);
            assertTrue(
                    hasInlinePrimaryKey(table),
                    "createTable " + table.getAttribute("tableName")
                            + " has no inline primary key column");
        }
    }

    private static Document loadChangeLog() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (InputStream in = LiquibaseQuartzInitXmlTest.class.getResourceAsStream(CHANGELOG)) {
            assertNotNull(in, "changelog resource not found on classpath: " + CHANGELOG);
            return builder.parse(in);
        }
    }

    private static boolean hasInlinePrimaryKey(Element createTable) {
        NodeList columns = createTable.getElementsByTagNameNS(ANY_NS, "column");
        for (int i = 0; i < columns.getLength(); i++) {
            Element column = (Element) columns.item(i);
            NodeList constraints = column.getElementsByTagNameNS(ANY_NS, "constraints");
            for (int j = 0; j < constraints.getLength(); j++) {
                if ("true".equalsIgnoreCase(((Element) constraints.item(j)).getAttribute("primaryKey"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
