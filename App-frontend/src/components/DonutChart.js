import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import Svg, { G, Circle } from 'react-native-svg';
import { Theme } from '../theme/Theme';

const DonutChart = ({ owe, get, net }) => {
  const radius = 75;
  const strokeWidth = 18;
  const colorOwe = Theme.colors.owe;
  const colorGet = Theme.colors.get;
  const colorBackground = Theme.colors.surface;
  
  const circumference = 2 * Math.PI * radius;
  
  const absOwe = Math.abs(owe);
  const absGet = Math.abs(get);
  const total = absOwe + absGet || 0.1;

  const owePercentage = (absOwe / total) * 100;
  const getPercentage = (absGet / total) * 100;

  const oweStroke = (owePercentage / 100) * circumference;
  const getStroke = (getPercentage / 100) * circumference;

  return (
    <View style={styles.container}>
      <Svg width={200} height={200} viewBox="0 0 200 200">
        <G rotation="-90" origin="100, 100">
          {/* Background Ring */}
          <Circle
            cx="100"
            cy="100"
            r={radius}
            stroke={colorBackground}
            strokeWidth={strokeWidth}
            fill="transparent"
          />
          {/* Owe Segment */}
          <Circle
            cx="100"
            cy="100"
            r={radius}
            stroke={colorOwe}
            strokeWidth={strokeWidth}
            strokeDasharray={`${oweStroke} ${circumference}`}
            strokeLinecap="round"
            fill="transparent"
          />
          {/* Get Segment */}
          {absGet > 0 && (
            <Circle
              cx="100"
              cy="100"
              r={radius}
              stroke={colorGet}
              strokeWidth={strokeWidth}
              strokeDasharray={`${getStroke} ${circumference}`}
              strokeDashoffset={-oweStroke}
              strokeLinecap="round"
              fill="transparent"
            />
          )}
        </G>
      </Svg>
      <View style={styles.textContainer}>
        <Text style={styles.label}>Net Balance</Text>
        <Text style={[styles.net, { color: net >= 0 ? Theme.colors.get : Theme.colors.owe }]}>
          ₹{Math.abs(net).toLocaleString('en-IN', { maximumFractionDigits: 0 })}
        </Text>
        <Text style={styles.status}>{net >= 0 ? "To Receive" : "To Settle"}</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: Theme.spacing.lg,
  },
  textContainer: {
    position: 'absolute',
    alignItems: 'center',
  },
  label: {
    fontSize: 10,
    color: Theme.colors.textSecondary,
    fontWeight: '700',
    textTransform: 'uppercase',
    letterSpacing: 1.5,
    marginBottom: 4,
  },
  net: {
    fontSize: 32,
    fontWeight: '900',
    letterSpacing: -1,
  },
  status: {
    fontSize: 12,
    color: Theme.colors.textSecondary,
    fontWeight: '500',
    marginTop: 2,
  },
});

export default DonutChart;
