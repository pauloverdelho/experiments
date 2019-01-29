
package com.ana.documents.service;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntervalService implements Serializable {

    public Map<IntervalType, String> getIntervals(String text) {
        Map<IntervalType, String> intervals = new HashMap<>();
        List<String> numbers = readNumbers(text);
        String facturas = getIntervals(getNumbersByType(IntervalType.FACTURA, numbers));
        if (facturas.length() > 0) {
            intervals.put(IntervalType.FACTURA, facturas);
        }

        String notasDeCredito = getIntervals(getNumbersByType(IntervalType.NOTA_DE_CREDITO, numbers));
        if (notasDeCredito.length() > 0) {
            intervals.put(IntervalType.NOTA_DE_CREDITO, notasDeCredito);
        }

        return intervals;
    }

    private static List<String> readNumbers(String text) {
        return new BufferedReader(new StringReader(text)).lines().collect(Collectors.toList());
    }

    private static String getIntervals(List<Integer> numbers) {
        StringBuilder intervals = new StringBuilder();
        Integer previous = null;
        boolean interval = false;

        Integer number;
        for (Iterator var4 = numbers.iterator(); var4.hasNext(); previous = number) {
            number = (Integer) var4.next();
            if (previous != null) {
                if (number - previous == 1) {
                    if (!interval) {
                        interval = true;
                        intervals.append('-');
                    }
                } else {
                    if (interval) {
                        intervals.append(previous);
                    }

                    intervals.append("; ").append(number);
                    interval = false;
                }
            } else {
                intervals.append(number);
            }
        }

        if (previous != null && interval) {
            intervals.append(previous);
        }

        return intervals.toString();
    }

    private static List<Integer> getNumbersByType(IntervalType type, List<String> numberList) {
        return numberList.stream().filter(s -> startsWithAny(s, type)).map(Integer::valueOf).distinct().sorted().collect(Collectors.toList());
    }

    private static boolean startsWithAny(String number, IntervalType type) {
        return Stream.of(type.numbers()).anyMatch(number::startsWith);
    }

    public static void main(String[] args) {
        String numbers = "9350123\n" +
                "9351123\n" +
                "9352123\n" +
                "9353123\n" +
                "9359123";
        IntervalService service = new IntervalService();
        Map<IntervalType, String> intervals = service.getIntervals(numbers);
        System.out.println(intervals);
    }
}
