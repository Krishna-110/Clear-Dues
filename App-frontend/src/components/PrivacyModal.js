import React from 'react';
import LegalModal from './LegalModal';
import { PRIVACY_SECTIONS, PRIVACY_LAST_UPDATED } from '../data/privacyContent';

const PrivacyModal = ({ visible, onClose }) => (
  <LegalModal
    visible={visible}
    onClose={onClose}
    title="Privacy Policy"
    updatedLabel={PRIVACY_LAST_UPDATED}
    sections={PRIVACY_SECTIONS}
  />
);

export default PrivacyModal;
