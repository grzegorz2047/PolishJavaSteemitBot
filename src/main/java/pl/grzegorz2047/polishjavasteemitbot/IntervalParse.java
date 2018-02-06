package pl.grzegorz2047.polishjavasteemitbot;

import java.util.ArrayList;
import java.util.List;

public class IntervalParse {

    public List<Interval> parse(String intervalProp) {
        List<Interval> intervals = new ArrayList<>();
        String[] intervalStrings = intervalProp.split(";");
        for (String singleIntervalString : intervalStrings) {
            String[] intervalAndVotingPower = singleIntervalString.split(",");
            if (intervalAndVotingPower.length != 2) {
                continue;
            }
            try {
                String[] intervalNumbers = intervalAndVotingPower[0].split("-");
                if (intervalNumbers.length != 2) {
                    continue;
                }
                float firstNumber = Float.parseFloat(intervalNumbers[0]);
                float secondNumber = Float.parseFloat(intervalNumbers[1]);
                float votingPower = Float.parseFloat(intervalAndVotingPower[1]);
                if (firstNumber > secondNumber) {
                    System.out.println("intervale error when " + firstNumber + "is bigger than " + secondNumber);
                    continue;
                }
                intervals.add(new Interval(firstNumber, secondNumber, votingPower));
            } catch (NumberFormatException ex) {
                continue;
            }

        }

        return intervals;
    }
}
