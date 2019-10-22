package com.ana.documents.service;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IntervalService implements Serializable {

    public Map<IntervalType, String> getIntervals(String text) {
        EnumMap<IntervalType, String> intervals = new EnumMap<>(IntervalType.class);
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
        for (Iterator iterator = numbers.iterator(); iterator.hasNext(); previous = number) {
            number = (Integer) iterator.next();
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
        return numberList.stream()
                .filter(s -> startsWithAny(s, type))
                .map(IntervalService::mapInteger)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static Optional<Integer> mapInteger(String number) {
        try {
            return Optional.of(Integer.valueOf(number));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean startsWithAny(String number, IntervalType type) {
        return Stream.of(type.numbers()).anyMatch(number::startsWith);
    }
}
