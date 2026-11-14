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

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Conversion between POSIX / Vixie-cron (cronie) style 5-field cron
 * expressions and Quartz {@link CronExpression}s.
 *
 * @see CronExpression#parsePosix(String)
 * @see CronExpression#toStringPosix()
 */
final class PosixCronSupport {

    private static final int MINUTE = 0;
    private static final int HOUR = 1;
    private static final int DAY_OF_MONTH = 2;
    private static final int MONTH = 3;
    private static final int DAY_OF_WEEK = 4;

    private static final String[] FIELD_NAMES = {"minute", "hour", "day-of-month", "month", "day-of-week"};
    private static final int[] MIN = {0, 0, 1, 1, 0};
    /** Highest accepted value. Day-of-week accepts 7 as an alias of 0 (Sunday). */
    private static final int[] MAX = {59, 23, 31, 12, 7};
    /** Highest value after normalization. */
    private static final int[] NORMALIZED_MAX = {59, 23, 31, 12, 6};

    private static final List<String> MONTH_NAMES =
            Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
    private static final List<String> DAY_NAMES =
            Arrays.asList("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");

    private static final Map<String, String> MACROS = new HashMap<>();
    static {
        MACROS.put("@YEARLY", "0 0 1 1 *");
        MACROS.put("@ANNUALLY", "0 0 1 1 *");
        MACROS.put("@MONTHLY", "0 0 1 * *");
        MACROS.put("@WEEKLY", "0 0 * * 0");
        MACROS.put("@DAILY", "0 0 * * *");
        MACROS.put("@MIDNIGHT", "0 0 * * *");
        MACROS.put("@HOURLY", "0 * * * *");
    }

    private PosixCronSupport() {
    }

    /**
     * Translates a POSIX / Vixie-cron expression into the equivalent Quartz
     * cron expression string.
     */
    static String toQuartz(String posixExpression) throws ParseException {
        if (posixExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null");
        }
        String expression = posixExpression.trim().toUpperCase(Locale.US);

        if (expression.startsWith("@")) {
            String expanded = MACROS.get(expression);
            if (expanded == null) {
                throw new ParseException("Unsupported cron macro: '" + posixExpression.trim() + "'", 0);
            }
            expression = expanded;
        }

        String[] fields = expression.isEmpty() ? new String[0] : expression.split("[ \t]+");
        if (fields.length != 5) {
            throw new ParseException(
                    "POSIX cron expression must have exactly 5 fields, found " + fields.length + ": '"
                            + posixExpression + "'", 0);
        }

        @SuppressWarnings("unchecked")
        TreeSet<Integer>[] sets = new TreeSet[5];
        for (int type = MINUTE; type <= DAY_OF_WEEK; type++) {
            sets[type] = parseField(fields[type], type);
        }

        TreeSet<Integer> dom = sets[DAY_OF_MONTH];
        TreeSet<Integer> dow = sets[DAY_OF_WEEK];
        boolean domFull = isFull(dom, DAY_OF_MONTH);
        boolean dowFull = isFull(dow, DAY_OF_WEEK);

        // Vixie-cron semantics: when both day fields are restricted (neither
        // starts with '*'), a day matches if EITHER field matches. Otherwise
        // a day must match BOTH fields.
        boolean domRestricted = !fields[DAY_OF_MONTH].startsWith("*");
        boolean dowRestricted = !fields[DAY_OF_WEEK].startsWith("*");
        boolean union = domRestricted && dowRestricted;

        String domPart;
        String dowPart;
        if (union ? (domFull || dowFull) : (domFull && dowFull)) {
            domPart = "*";
            dowPart = "?";
        } else if (!union && dowFull) {
            domPart = format(dom, DAY_OF_MONTH);
            dowPart = "?";
        } else if (!union && domFull) {
            domPart = "?";
            dowPart = format(toQuartzDaysOfWeek(dow), DAY_OF_WEEK, 1);
        } else {
            throw new ParseException(
                    "Support for restricting both day-of-month and day-of-week is not implemented: '"
                            + posixExpression + "'", 0);
        }

        return "0 " + format(sets[MINUTE], MINUTE) + " " + format(sets[HOUR], HOUR) + " " + domPart + " "
                + format(sets[MONTH], MONTH) + " " + dowPart;
    }

    /**
     * Renders the given Quartz expression as a POSIX / Vixie-cron expression.
     *
     * @throws UnsupportedOperationException if the expression uses features
     *         that cannot be expressed in the 5-field format
     */
    static String toPosix(CronExpression cron) throws UnsupportedOperationException {
        if (!cron.seconds.equals(new TreeSet<>(Arrays.asList(0)))) {
            throw unsupported(cron, "it fires at seconds other than 0");
        }
        if (!cron.years.contains(CronExpression.ALL_SPEC)) {
            throw unsupported(cron, "it restricts the year");
        }
        if (!cron.nearestWeekdays.isEmpty()) {
            throw unsupported(cron, "it uses 'W' (nearest weekday)");
        }
        if (cron.lastDayOfWeek || cron.daysOfMonth.stream().anyMatch(d -> d > 31 && d != CronExpression.ALL_SPEC_INT
                && d != CronExpression.NO_SPEC_INT)) {
            throw unsupported(cron, "it uses 'L' (last day)");
        }
        if (cron.nthDayOfWeek != 0) {
            throw unsupported(cron, "it uses '#' (nth day of week)");
        }

        TreeSet<Integer> dow = new TreeSet<>();
        for (int quartzDay : values(cron.daysOfWeek)) {
            dow.add(quartzDay - 1); // Quartz SUN=1..SAT=7, POSIX SUN=0..SAT=6
        }

        return format(values(cron.minutes), MINUTE) + " " + format(values(cron.hours), HOUR) + " "
                + (cron.daysOfMonth.contains(CronExpression.NO_SPEC) ? "*" : format(values(cron.daysOfMonth),
                DAY_OF_MONTH)) + " " + format(values(cron.months), MONTH) + " "
                + (cron.daysOfWeek.contains(CronExpression.NO_SPEC) ? "*" : format(dow, DAY_OF_WEEK));
    }

    private static UnsupportedOperationException unsupported(CronExpression cron, String reason) {
        return new UnsupportedOperationException(
                "Cron expression '" + cron.getCronExpression() + "' cannot be represented in POSIX format because "
                        + reason);
    }

    /** Copy of the set without the '*' and '?' markers. */
    private static TreeSet<Integer> values(SortedSet<Integer> set) {
        TreeSet<Integer> result = new TreeSet<>(set);
        result.remove(CronExpression.ALL_SPEC);
        result.remove(CronExpression.NO_SPEC);
        return result;
    }

    private static TreeSet<Integer> toQuartzDaysOfWeek(SortedSet<Integer> posixDays) {
        TreeSet<Integer> result = new TreeSet<>();
        for (int day : posixDays) {
            result.add(day + 1);
        }
        return result;
    }

    // ------------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------------

    private static TreeSet<Integer> parseField(String field, int type) throws ParseException {
        TreeSet<Integer> result = new TreeSet<>();
        for (String element : field.split(",", -1)) {
            parseElement(element, field, type, result);
        }
        return result;
    }

    private static void parseElement(String element, String field, int type, TreeSet<Integer> result)
            throws ParseException {
        if (element.isEmpty()) {
            throw invalid(field, type, "empty list element");
        }

        String range = element;
        int step = 1;
        int slash = element.indexOf('/');
        if (slash >= 0) {
            range = element.substring(0, slash);
            step = parseNumber(element.substring(slash + 1), field, type);
            if (step < 1 || step > MAX[type]) {
                throw invalid(field, type, "step must be between 1 and " + MAX[type]);
            }
        }

        int start;
        int end;
        if (range.equals("*")) {
            start = MIN[type];
            end = NORMALIZED_MAX[type];
        } else {
            int dash = range.indexOf('-');
            if (dash >= 0) {
                start = parseValue(range.substring(0, dash), field, type);
                end = parseValue(range.substring(dash + 1), field, type);
                if (start > end) {
                    throw invalid(field, type, "range start is greater than range end");
                }
            } else {
                start = parseValue(range, field, type);
                // "N/step" is a common extension meaning "N-max/step".
                end = slash >= 0 ? NORMALIZED_MAX[type] : start;
            }
        }

        for (int value = start; value <= end; value += step) {
            result.add(type == DAY_OF_WEEK ? value % 7 : value);
        }
    }

    private static int parseValue(String token, String field, int type) throws ParseException {
        List<String> names = type == MONTH ? MONTH_NAMES : type == DAY_OF_WEEK ? DAY_NAMES : null;
        if (names != null) {
            int index = names.indexOf(token);
            if (index >= 0) {
                return type == MONTH ? index + 1 : index;
            }
        }
        int value = parseNumber(token, field, type);
        if (value < MIN[type] || value > MAX[type]) {
            throw invalid(field, type, "values must be between " + MIN[type] + " and " + MAX[type]);
        }
        return value;
    }

    private static int parseNumber(String token, String field, int type) throws ParseException {
        if (token.isEmpty() || token.length() > 4 || !token.chars().allMatch(c -> c >= '0' && c <= '9')) {
            throw invalid(field, type, "'" + token + "' is not a valid value");
        }
        return Integer.parseInt(token);
    }

    private static ParseException invalid(String field, int type, String reason) {
        return new ParseException("Invalid " + FIELD_NAMES[type] + " field '" + field + "': " + reason, 0);
    }

    // ------------------------------------------------------------------------
    // Formatting (shared by both directions)
    // ------------------------------------------------------------------------

    private static boolean isFull(SortedSet<Integer> values, int type) {
        return values.size() == NORMALIZED_MAX[type] - MIN[type] + 1;
    }

    /**
     * Formats a set of values as a compact field: '*', '*&#47;step',
     * 'a-b/step', ranges and lists. {@code offset} shifts the field bounds,
     * used for the Quartz day-of-week numbering (SUN=1).
     */
    private static String format(SortedSet<Integer> values, int type) {
        return format(values, type, 0);
    }

    private static String format(SortedSet<Integer> values, int type, int offset) {
        int min = MIN[type] + offset;
        int max = NORMALIZED_MAX[type] + offset;
        if (values.size() == max - min + 1) {
            return "*";
        }

        Integer[] v = values.toArray(new Integer[0]);
        if (v.length >= 3) {
            int step = v[1] - v[0];
            boolean progression = step > 1;
            for (int i = 2; progression && i < v.length; i++) {
                progression = v[i] - v[i - 1] == step;
            }
            if (progression) {
                if (v[0] == min && v[v.length - 1] + step > max) {
                    return "*/" + step;
                }
                return v[0] + "-" + v[v.length - 1] + "/" + step;
            }
        }

        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < v.length) {
            int j = i;
            while (j + 1 < v.length && v[j + 1] == v[j] + 1) {
                j++;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            if (j - i >= 2) {
                sb.append(v[i]).append('-').append(v[j]);
                i = j + 1;
            } else {
                sb.append(v[i]);
                i++;
            }
        }
        return sb.toString();
    }
}
