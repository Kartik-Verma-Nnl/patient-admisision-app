// Frontend validation. The backend repeats the same rules.
const todayStr = () => {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
};
export const today = todayStr;

export function validatePatient(v) {
  const e = {};

  const name = (v.name ?? '').trim();
  if (!name) e.name = 'Name is required';
  else if (!/^[\p{L}]+(?:[ .'-][\p{L}]+)*$/u.test(name)) e.name = 'Name must contain letters only';

  if (v.age === '' || v.age === null || v.age === undefined) e.age = 'Age is required';
  else if (!/^\d+$/.test(String(v.age))) e.age = 'Age must be a whole number';
  else if (Number(v.age) < 0 || Number(v.age) > 120) e.age = 'Age must be between 0 and 120';

  ['gender', 'ward', 'department', 'doctor'].forEach((f) => {
    if (!v[f]) e[f] = `${f.charAt(0).toUpperCase() + f.slice(1)} is required`;
  });

  if (!v.admissionDate) e.admissionDate = 'Admission date is required';
  else if (v.admissionDate > todayStr()) e.admissionDate = 'Admission date cannot be in the future';

  return e;
}
