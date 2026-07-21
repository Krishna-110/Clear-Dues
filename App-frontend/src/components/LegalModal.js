import React from 'react';
import { View, Text, StyleSheet, Modal, ScrollView, TouchableOpacity } from 'react-native';
import { Theme } from '../theme/Theme';
import { X } from 'lucide-react-native';

// Generic bottom-sheet reader for a legal document (Terms, Privacy, ...), rendered from
// structured section data rather than a WebView. `sections` bullets may be a plain string
// or { label, text } for a bold lead-in label.
const LegalModal = ({ visible, onClose, title, updatedLabel, sections }) => (
  <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
    <View style={styles.overlay}>
      <View style={styles.content}>
        <View style={styles.header}>
          <Text style={styles.title}>{title}</Text>
          <TouchableOpacity style={styles.closeBtn} onPress={onClose}>
            <X size={20} color={Theme.colors.textSecondary} />
          </TouchableOpacity>
        </View>
        {!!updatedLabel && <Text style={styles.updated}>Last updated {updatedLabel}</Text>}

        <ScrollView style={styles.scroll} showsVerticalScrollIndicator={false}>
          {sections.map((section, index) => (
            <View key={index} style={styles.section}>
              <Text style={styles.sectionTitle}>{section.title}</Text>
              {section.body && <Text style={styles.sectionBody}>{section.body}</Text>}
              {section.bullets && section.bullets.map((bullet, i) => {
                const hasLabel = typeof bullet === 'object' && bullet !== null;
                return (
                  <Text key={i} style={styles.bullet}>
                    {'• '}
                    {hasLabel && <Text style={styles.bulletLabel}>{bullet.label}: </Text>}
                    {hasLabel ? bullet.text : bullet}
                  </Text>
                );
              })}
            </View>
          ))}
        </ScrollView>

        <TouchableOpacity style={styles.doneBtn} onPress={onClose}>
          <Text style={styles.doneText}>Close</Text>
        </TouchableOpacity>
      </View>
    </View>
  </Modal>
);

const styles = StyleSheet.create({
  overlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
  content: {
    backgroundColor: Theme.colors.white, borderTopLeftRadius: 30, borderTopRightRadius: 30,
    padding: Theme.spacing.xl, paddingBottom: 32, maxHeight: '85%',
  },
  header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  title: { fontSize: 20, fontWeight: '800', color: Theme.colors.text },
  closeBtn: {
    width: 32, height: 32, borderRadius: 16, backgroundColor: Theme.colors.surface,
    alignItems: 'center', justifyContent: 'center',
  },
  updated: { fontSize: 12, color: Theme.colors.textSecondary, marginTop: 4, marginBottom: Theme.spacing.md },
  scroll: { marginBottom: Theme.spacing.md },
  section: { marginBottom: Theme.spacing.lg },
  sectionTitle: { fontSize: 15, fontWeight: '800', color: Theme.colors.text, marginBottom: 6 },
  sectionBody: { fontSize: 14, color: Theme.colors.textSecondary, lineHeight: 21 },
  bullet: { fontSize: 14, color: Theme.colors.textSecondary, lineHeight: 21, marginTop: 4 },
  bulletLabel: { fontWeight: '800', color: Theme.colors.text },
  doneBtn: {
    backgroundColor: Theme.colors.primary, borderRadius: Theme.borderRadius.lg,
    alignItems: 'center', paddingVertical: 14,
  },
  doneText: { color: Theme.colors.white, fontWeight: '800', fontSize: 16 },
});

export default LegalModal;
