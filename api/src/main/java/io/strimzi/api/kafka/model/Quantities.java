/*
 * Copyright 2018, Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.api.kafka.model;

public class Quantities {
    private Quantities() {

    }

    /**
     * Parse a K8S-style representation of a quantity of memory, such as {@code 512Mi},
     * into the equivalent number of bytes represented as a long.
     * @param memory The String representation of the quantity of memory.
     * @return The equivalent number of bytes.
     */
    public static long parseMemory(String memory) {
        boolean seenE = false;
        long factor = 1L;
        int end = memory.length();
        for (int i = 0; i < memory.length(); i++) {
            char ch = memory.charAt(i);
            if (ch == 'e') {
                seenE = true;
            } else if (ch < '0' || '9' < ch) {
                end = i;
                factor = memoryFactor(memory.substring(i));
                break;
            }
        }
        long result;
        if (seenE) {
            result = (long) Double.parseDouble(memory);
        } else {
            result = Long.parseLong(memory.substring(0, end)) * factor;
        }
        return result;
    }

    private static long memoryFactor(String suffix) {
        long factor;
        switch (suffix) {
            case "E":
                factor = 1_000L * 1_000L * 1_000L * 1_000 * 1_000L;
                break;
            case "T":
                factor = 1_000L * 1_000L * 1_000L * 1_000;
                break;
            case "G":
                factor = 1_000L * 1_000L * 1_000L;
                break;
            case "M":
                factor = 1_000L * 1_000L;
                break;
            case "K":
                factor = 1_000L;
                break;
            case "Ei":
                factor = 1_024L * 1_024L * 1_024L * 1_024L * 1_024L;
                break;
            case "Ti":
                factor = 1_024L * 1_024L * 1_024L * 1_024L;
                break;
            case "Gi":
                factor = 1_024L * 1_024L * 1_024L;
                break;
            case "Mi":
                factor = 1_024L * 1_024L;
                break;
            case "Ki":
                factor = 1_024L;
                break;
            default:
                throw new IllegalArgumentException("Invalid memory suffix: " + suffix);
        }
        return factor;
    }

    public static String formatMemory(long bytes) {
        if (bytes == 0) {
            return "0";
        }
        long x;
        int i;
        x = bytes;
        i = -1;
        while ((x % 1000L) == 0L && i < 4) {
            i++;
            x = x / 1000L;
        }
        if (i >= 0) {
            return x + new String[] {"K", "M", "G", "T", "E"}[i];
        }
        x = bytes;
        i = -1;
        while ((x % 1024L) == 0L && i < 4) {
            i++;
            x = x / 1024L;
        }
        if (i >= 0) {
            return x + new String[]{"Ki", "Mi", "Gi", "Ti", "Ei"}[i];
        }
        return Long.toString(bytes);
    }

    public static String normalizeMemory(String memory) {
        return formatMemory(parseMemory(memory));
    }


    /**
     * Parse a K8S-style representation of a quantity of cpu, such as {@code 1000m},
     * into the equivalent number of "millicpus" represented as an int.
     * @param cpu The String representation of the quantity of cpu.
     * @return The equivalent number of "millicpus".
     */
    public static int parseCpuAsMilliCpus(String cpu) {
        int suffixIndex = cpu.length();
        int factor = 1000;
        for (int i = 0; i < cpu.length(); i++) {
            char ch = cpu.charAt(i);
            if (ch < '0' || '9' < ch) {
                suffixIndex = i;
                if ("m".equals(cpu.substring(i))) {
                    factor = 1;
                    break;
                } else if (cpu.substring(i).startsWith(".")) {
                    return (int) (Double.parseDouble(cpu) * 1000L);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        return factor * Integer.parseInt(cpu.substring(0, suffixIndex));
    }

    public static String formatMilliCpu(int milliCpu) {
        if (milliCpu % 1000 == 0) {
            return Long.toString(milliCpu / 1000L);
        } else {
            return Long.toString(milliCpu) + "m";
        }
    }

    public static String normalizeCpu(String milliCpu) {
        return formatMilliCpu(parseCpuAsMilliCpus(milliCpu));
    }
}
