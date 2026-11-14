/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
package org.quartz;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class CronExpressionPosixTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // POSIX / Vixie expression | Quartz toString() | toStringPosix()
            "* * * * *                  | 0 * * * * ?              | * * * * *",
            "*/15 9-17 * * MON-FRI      | 0 */15 9-17 ? * 2-6      | */15 9-17 * * 1-5",
            "30 2 * * 0                 | 0 30 2 ? * 1             | 30 2 * * 0",
            "30 2 * * 7                 | 0 30 2 ? * 1             | 30 2 * * 0",
            "0 0 * * sat,sun            | 0 0 0 ? * 1,7            | 0 0 * * 0,6",
            "0 0 * * 5-7                | 0 0 0 ? * 1,6,7          | 0 0 * * 0,5,6",
            "0 0 * * 0-7                | 0 0 0 * * ?              | 0 0 * * *",
            "0 0 * * */2                | 0 0 0 ? * */2            | 0 0 * * */2",
            "5/15 * * * *               | 0 5-50/15 * * * ?        | 5-50/15 * * * *",
            "0-30/10 * * * *            | 0 0-30/10 * * * ?        | 0-30/10 * * * *",
            "0 12 1 JAN,jul *           | 0 0 12 1 1,7 ?           | 0 12 1 1,7 *",
            "0 12 1 JAN-JUN/2 *         | 0 0 12 1 1-5/2 ?         | 0 12 1 1-5/2 *",
            "0 0 */2 * *                | 0 0 0 */2 * ?            | 0 0 */2 * *",
            "1,2,3,10 0 1,15 * *        | 0 1-3,10 0 1,15 * ?      | 1-3,10 0 1,15 * *",
            "0 0 1-31 * MON             | 0 0 0 * * ?              | 0 0 * * *",
            "0 0 1 * *                  | 0 0 0 1 * ?              | 0 0 1 * *",
            "@yearly                    | 0 0 0 1 1 ?              | 0 0 1 1 *",
            "@annually                  | 0 0 0 1 1 ?              | 0 0 1 1 *",
            "@monthly                   | 0 0 0 1 * ?              | 0 0 1 * *",
            "@weekly                    | 0 0 0 ? * 1              | 0 0 * * 0",
            "@daily                     | 0 0 0 * * ?              | 0 0 * * *",
            "@midnight                  | 0 0 0 * * ?              | 0 0 * * *",
            "@hourly                    | 0 0 * * * ?              | 0 * * * *",
            "'  0\t12  *  * * '         | 0 0 12 * * ?             | 0 12 * * *",
    })
    void parsesPosixExpressions(String posix, String quartz, String canonicalPosix) throws ParseException {
        CronExpression expression = CronExpression.parsePosix(posix);

        assertEquals(quartz, expression.toString());
        assertEquals(canonicalPosix, expression.toStringPosix());
        assertEquals(quartz, CronExpression.parsePosix(canonicalPosix).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "* * * *",
            "* * * * * *",
            "60 * * * *",
            "* 24 * * *",
            "* * 0 * *",
            "* * 32 * *",
            "* * * 0 *",
            "* * * 13 *",
            "* * * * 8",
            "5-1 * * * *",
            "*/0 * * * *",
            "*/61 * * * *",
            "a * * * *",
            "1,,2 * * * *",
            "1- * * * *",
            "-1 * * * *",
            "* * * FOO *",
            "* * * * MONDAY",
            "@reboot",
            "@every_minute",
            // both day-of-month and day-of-week restricted: OR semantics in cron
            "0 0 1 * MON",
            "0 0 1,15 * 1-5",
            // day-of-month step with restricted day-of-week: AND semantics in cron
            "0 0 */2 * MON",
            "0 0 1 * */2",
    })
    void rejectsInvalidOrUnsupportedExpressions(String posix) {
        assertThrows(ParseException.class, () -> CronExpression.parsePosix(posix));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> CronExpression.parsePosix(null));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "*/15 9-17 * * MON-FRI | 0 0/15 9-17 ? * MON-FRI",
            "0 0 * * 0             | 0 0 0 ? * SUN",
            "0 0 1 */3 *           | 0 0 0 1 1/3 ?",
            "45 23 * * 5-7         | 0 45 23 ? * FRI-SUN",
    })
    void firesAtTheSameTimesAsTheQuartzEquivalent(String posix, String quartz) throws ParseException {
        CronExpression fromPosix = CronExpression.parsePosix(posix);
        CronExpression fromQuartz = new CronExpression(quartz);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        fromPosix.setTimeZone(utc);
        fromQuartz.setTimeZone(utc);

        Date posixTime = new Date(1_700_000_000_000L);
        Date quartzTime = posixTime;
        for (int i = 0; i < 200; i++) {
            posixTime = fromPosix.getNextValidTimeAfter(posixTime);
            quartzTime = fromQuartz.getNextValidTimeAfter(quartzTime);
            assertEquals(quartzTime, posixTime);
        }
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "0 0/5 14,18 * * ?       | */5 14,18 * * *",
            "0 15 10 ? * MON-FRI     | 15 10 * * 1-5",
            "0 15 10 15 * ?          | 15 10 15 * *",
            "0 0 12 1/5 * ?          | 0 12 */5 * *",
            "0 11 11 11 11 ?         | 11 11 11 11 *",
            "0 0 12 * * ? *          | 0 12 * * *",
            "0 0 22-2 * * ?          | 0 0-2,22,23 * * *",
    })
    void rendersQuartzExpressionsAsPosix(String quartz, String posix) throws ParseException {
        assertEquals(posix, new CronExpression(quartz).toStringPosix());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "30 * * * * ?",
            "* * * * * ?",
            "0 0 12 * * ? 2025",
            "0 15 10 L * ?",
            "0 15 10 L-2 * ?",
            "0 15 10 15W * ?",
            "0 15 10 LW * ?",
            "0 15 10 ? * 6L",
            "0 15 10 ? * 6#3",
    })
    void cannotRenderQuartzOnlyFeaturesAsPosix(String quartz) throws ParseException {
        CronExpression expression = new CronExpression(quartz);
        assertThrows(UnsupportedOperationException.class, expression::toStringPosix);
    }
}
