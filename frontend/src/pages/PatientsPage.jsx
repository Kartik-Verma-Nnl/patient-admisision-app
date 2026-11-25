import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert, Box, Button, Card, CardContent, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogContentText, DialogTitle, Grid, IconButton, InputAdornment, MenuItem,
  Paper, Snackbar, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Tooltip, Typography, useMediaQuery, useTheme,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import SearchIcon from '@mui/icons-material/Search';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import ExitToAppIcon from '@mui/icons-material/ExitToApp';
import PatientFormDialog from '../components/PatientFormDialog';
import { patientApi } from '../services/patientApi';
import { OPTIONS } from '../config/options';


export default function PatientsPage() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  const [patients, setPatients] = useState([]);
  const options = OPTIONS;
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [toast, setToast] = useState(null);

  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState({ ward: '', department: '', doctor: '' });

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [toDelete, setToDelete] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError('');
    try {
      setPatients(await patientApi.list());
    } catch (e) {
      setLoadError(e.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const visible = useMemo(() => {
    const q = search.trim().toLowerCase();
    return patients.filter((p) =>
      (!q || p.name.toLowerCase().includes(q)) &&
      (!filters.ward || p.ward === filters.ward) &&
      (!filters.department || p.department === filters.department) &&
      (!filters.doctor || p.doctor === filters.doctor));
  }, [patients, search, filters]);

  const notify = (message, severity = 'success') => setToast({ message, severity });

  const handleSave = async (data) => {
    if (editing) await patientApi.update(editing.id, data);
    else await patientApi.create(data);
    setFormOpen(false);
    notify(editing ? 'Patient updated' : 'Patient added');
    load();
  };

  const handleDischarge = async (p) => {
    try {
      await patientApi.discharge(p.id);
      notify(`${p.name} marked as discharged`);
      load();
    } catch (e) { notify(e.message, 'error'); }
  };

  const handleDelete = async () => {
    try {
      await patientApi.remove(toDelete.id);
      notify('Patient deleted');
      setToDelete(null);
      load();
    } catch (e) { notify(e.message, 'error'); setToDelete(null); }
  };

  const openAdd = () => { setEditing(null); setFormOpen(true); };
  const openEdit = (p) => { setEditing(p); setFormOpen(true); };

  const statusChip = (s) => (
    <Chip size="small" label={s} color={s === 'Admitted' ? 'success' : 'default'} variant={s === 'Admitted' ? 'filled' : 'outlined'} />
  );

  const actions = (p) => (
    <Stack direction="row" spacing={0.5} justifyContent="flex-end">
      {p.status === 'Admitted' && (
        <Tooltip title="Mark as discharged">
          <IconButton aria-label="Mark as discharged" onClick={() => handleDischarge(p)}><ExitToAppIcon /></IconButton>
        </Tooltip>
      )}
      <Tooltip title="Edit">
        <IconButton aria-label="Edit patient" onClick={() => openEdit(p)}><EditIcon /></IconButton>
      </Tooltip>
      <Tooltip title="Delete">
        <IconButton aria-label="Delete patient" color="error" onClick={() => setToDelete(p)}><DeleteIcon /></IconButton>
      </Tooltip>
    </Stack>
  );

  const filterSelect = (field, label, list) => (
    <TextField
      select fullWidth size="small" label={label} value={filters[field]}
      onChange={(e) => setFilters((f) => ({ ...f, [field]: e.target.value }))}
    >
      <MenuItem value="">All</MenuItem>
      {list.map((o) => <MenuItem key={o} value={o}>{o}</MenuItem>)}
    </TextField>
  );

  return (
    <Box>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={1.5} mb={2}>
        <Box>
          <Typography variant="h5" fontWeight={700}>Patients</Typography>
          <Typography variant="body2" color="text.secondary">{visible.length} of {patients.length} shown</Typography>
        </Box>
        <Button variant="contained" startIcon={<AddIcon />} onClick={openAdd}>Add patient</Button>
      </Stack>

      <Paper variant="outlined" sx={{ p: 2, mb: 2 }}>
        <Grid container spacing={1.5}>
          <Grid item xs={12} md={3}>
            <TextField
              fullWidth size="small" label="Search by name" value={search}
              onChange={(e) => setSearch(e.target.value)}
              InputProps={{ startAdornment: <InputAdornment position="start"><SearchIcon fontSize="small" /></InputAdornment> }}
            />
          </Grid>
          <Grid item xs={12} sm={4} md={3}>{filterSelect('ward', 'Ward', options.wards)}</Grid>
          <Grid item xs={12} sm={4} md={3}>{filterSelect('department', 'Department', options.departments)}</Grid>
          <Grid item xs={12} sm={4} md={3}>{filterSelect('doctor', 'Doctor', options.doctors)}</Grid>
        </Grid>
      </Paper>

      {loadError && <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" onClick={load}>Retry</Button>}>{loadError}</Alert>}

      {loading ? (
        <Box textAlign="center" py={6}><CircularProgress /></Box>
      ) : visible.length === 0 ? (
        <Paper variant="outlined" sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">No patients found.</Typography>
        </Paper>
      ) : isMobile ? (
        // Mobile + tablet: cards
        <Grid container spacing={1.5}>
          {visible.map((p) => (
            <Grid item xs={12} sm={6} key={p.id}>
              <Card variant="outlined">
                <CardContent>
                  <Stack direction="row" justifyContent="space-between" alignItems="center">
                    <Typography fontWeight={600}>{p.name}</Typography>
                    {statusChip(p.status)}
                  </Stack>
                  <Typography variant="body2" color="text.secondary" mt={0.5}>
                    {p.age} yrs · {p.gender}
                  </Typography>
                  <Typography variant="body2" mt={1}>{p.ward} · {p.department}</Typography>
                  <Typography variant="body2">{p.doctor}</Typography>
                  <Typography variant="caption" color="text.secondary">Admitted {p.admissionDate}</Typography>
                  <Box mt={1}>{actions(p)}</Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      ) : (
        // Desktop: table
        <TableContainer component={Paper} variant="outlined">
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: 'action.hover' }}>
                {['Name', 'Age', 'Gender', 'Ward', 'Department', 'Doctor', 'Admission date', 'Status'].map((h) => (
                  <TableCell key={h} sx={{ fontWeight: 600 }}>{h}</TableCell>
                ))}
                <TableCell align="right" sx={{ fontWeight: 600 }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {visible.map((p) => (
                <TableRow key={p.id} hover>
                  <TableCell>{p.name}</TableCell>
                  <TableCell>{p.age}</TableCell>
                  <TableCell>{p.gender}</TableCell>
                  <TableCell>{p.ward}</TableCell>
                  <TableCell>{p.department}</TableCell>
                  <TableCell>{p.doctor}</TableCell>
                  <TableCell>{p.admissionDate}</TableCell>
                  <TableCell>{statusChip(p.status)}</TableCell>
                  <TableCell align="right">{actions(p)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <PatientFormDialog
        open={formOpen} patient={editing} options={options}
        onClose={() => setFormOpen(false)} onSubmit={handleSave}
      />

      <Dialog open={!!toDelete} onClose={() => setToDelete(null)}>
        <DialogTitle>Delete patient?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            This will permanently delete <b>{toDelete?.name}</b>. This cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setToDelete(null)}>Cancel</Button>
          <Button color="error" variant="contained" onClick={handleDelete}>Delete</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={!!toast} autoHideDuration={3500} onClose={() => setToast(null)} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        {toast ? <Alert severity={toast.severity} variant="filled" onClose={() => setToast(null)}>{toast.message}</Alert> : undefined}
      </Snackbar>
    </Box>
  );
}
