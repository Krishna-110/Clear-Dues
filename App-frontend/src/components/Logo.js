import React from 'react';
import { SvgXml } from 'react-native-svg';

// Settlement brand mark, rendered as a vector so it stays crisp at any size.
const LOGO_XML = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 500 500" width="100%" height="100%">
  <rect width="100%" height="100%" fill="#1A1D24" rx="100"/>
  <path d="M240 135 L355 335 L125 335 Z" fill="none" stroke="#232731" stroke-width="20" stroke-linejoin="round"/>
  <path d="M240 135 L355 335 L125 335 Z" fill="none" stroke="#64DFDF" stroke-width="4" stroke-linejoin="round" stroke-dasharray="8 8" opacity="0.7"/>
  <line x1="240" y1="135" x2="240" y2="268" stroke="#64DFDF" stroke-width="4" opacity="0.5"/>
  <line x1="355" y1="335" x2="240" y2="268" stroke="#64DFDF" stroke-width="4" opacity="0.5"/>
  <line x1="125" y1="335" x2="240" y2="268" stroke="#64DFDF" stroke-width="4" opacity="0.5"/>
  <circle cx="240" cy="135" r="14" fill="#232731" stroke="#94A3B8" stroke-width="4"/>
  <circle cx="355" cy="335" r="14" fill="#232731" stroke="#94A3B8" stroke-width="4"/>
  <circle cx="125" cy="335" r="14" fill="#232731" stroke="#94A3B8" stroke-width="4"/>
  <circle cx="240" cy="268" r="22" fill="#64DFDF"/>
  <circle cx="240" cy="268" r="8" fill="#1A1D24"/>
</svg>`;

const Logo = ({ size = 200, style }) => (
  <SvgXml xml={LOGO_XML} width={size} height={size} style={style} />
);

export default Logo;
