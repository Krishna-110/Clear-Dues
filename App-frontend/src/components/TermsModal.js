import React from 'react';
import LegalModal from './LegalModal';
import { TERMS_SECTIONS, TERMS_LAST_UPDATED } from '../data/termsContent';

const TermsModal = ({ visible, onClose }) => (
  <LegalModal
    visible={visible}
    onClose={onClose}
    title="Terms & Conditions"
    updatedLabel={TERMS_LAST_UPDATED}
    sections={TERMS_SECTIONS}
  />
);

export default TermsModal;
