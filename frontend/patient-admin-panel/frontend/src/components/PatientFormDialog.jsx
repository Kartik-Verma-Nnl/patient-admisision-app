import { useEffect, useState } from 'react';
import {
  Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, Grid, MenuItem, TextField,
} from '@mui/material';
import { validatePatient, today } from '../utils/validation';
import { ApiError } from '../services/patientApi';

const EMPTY = { name: '', age: '', gender: '', ward: '', department: '', doctor: '', admissionDate: '' };

export default function PatientFormDialog({ open, patient, options, onClose, onSubmit }) {
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setValues(patient ? { ...EMPTY, ...patient } : EMPTY);
      setErrors({});
      setServerError('');
    }
  }, [open, patient]);

  const set = (field) => (e) => {
    setValues((v) => ({ ...v, [field]: e.target.value }));
    setErrors((er) => ({ ...er, [field]: undefined }));
  };

  const handleSubmit = async () => {
    const found = validatePatient(values);
    setErrors(found);
    if (Object.keys(found).length) return;
    setSaving(true);
    setServerError('');
    try {
      await onSubmit({ ...values, name: values.name.trim(), age: Number(values.age) });
    } catch (err) {
      // Show backend validation messages on the fields
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors);
        setServerError(err.message);
      } else throw err;
    } finally {
      setSaving(false);
    }
  };

  const select = (field, label, list) => (
    <TextField
      select fullWidth required label={label} value={values[field]} onChange={set(field)}
      error={!!errors[field]} helperText={errors[field]}
    >
      {list.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
    </TextField>
  );

  return (
    <Dialog open={open} onClose={onClose} fullWidth maxWidth="sm">
      <DialogTitle>{patient ? 'Edit patient' : 'Add patient'}</DialogTitle>
      <DialogContent>
        {serverError && <Alert severity="error" sx={{ mb: 2 }}>{serverError}</Alert>}
        <Grid container spacing={2} sx={{ mt: 0 }}>
          <Grid item xs={12} sm={8}>
            <TextField
              fullWidth required label="Name" value={values.name} onChange={set('name')}
              error={!!errors.name} helperText={errors.name}
            />
          </Grid>
          <Grid item xs={12} sm={4}>
            <TextField
              fullWidth required label="Age" type="number" value={values.age} onChange={set('age')}
              inputProps={{ min: 0, max: 120 }} error={!!errors.age} helperText={errors.age}
            />
          </Grid>
          <Grid item xs={12} sm={6}>{select('gender', 'Gender', options.genders)}</Grid>
          <Grid item xs={12} sm={6}>{select('ward', 'Ward', options.wards)}</Grid>
          <Grid item xs={12} sm={6}>{select('department', 'Department', options.departments)}</Grid>
          <Grid item xs={12} sm={6}>{select('doctor', 'Doctor', options.doctors)}</Grid>
          <Grid item xs={12} sm={6}>
            <TextField
              fullWidth required label="Admission date" type="date" value={values.admissionDate}
              onChange={set('admissionDate')} InputLabelProps={{ shrink: true }}
              inputProps={{ max: today() }} error={!!errors.admissionDate} helperText={errors.admissionDate}
            />
          </Grid>
        </Grid>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={saving}>Cancel</Button>
        <Button variant="contained" onClick={handleSubmit} disabled={saving}>
          {saving ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
