import { useState } from 'react';
import {
  AppBar, Box, Drawer, IconButton, List, ListItemButton, ListItemIcon, ListItemText,
  Toolbar, Typography, useMediaQuery, useTheme,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import PeopleIcon from '@mui/icons-material/People';
import LocalHospitalIcon from '@mui/icons-material/LocalHospital';
import PatientsPage from './pages/PatientsPage';

const DRAWER_WIDTH = 230;

export default function App() {
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const [open, setOpen] = useState(false);

  const nav = (
    <Box>
      <Toolbar sx={{ gap: 1 }}>
        <LocalHospitalIcon color="primary" />
        <Typography variant="h6" fontWeight={700}>CareDesk</Typography>
      </Toolbar>
      <List>
        <ListItemButton selected>
          <ListItemIcon><PeopleIcon /></ListItemIcon>
          <ListItemText primary="Patients" />
        </ListItemButton>
      </List>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="inherit"
        elevation={0}
        sx={{ borderBottom: 1, borderColor: 'divider', ml: { md: `${DRAWER_WIDTH}px` }, width: { md: `calc(100% - ${DRAWER_WIDTH}px)` } }}
      >
        <Toolbar>
          {!isDesktop && (
            <IconButton edge="start" aria-label="Open menu" onClick={() => setOpen(true)} sx={{ mr: 1 }}>
              <MenuIcon />
            </IconButton>
          )}
          <Typography variant="h6" fontWeight={600}>Patient Management</Typography>
        </Toolbar>
      </AppBar>

      <Drawer
        variant={isDesktop ? 'permanent' : 'temporary'}
        open={isDesktop || open}
        onClose={() => setOpen(false)}
        ModalProps={{ keepMounted: true }}
        sx={{ width: { md: DRAWER_WIDTH }, '& .MuiDrawer-paper': { width: DRAWER_WIDTH } }}
      >
        {nav}
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: { xs: 1.5, sm: 3 }, minWidth: 0 }}>
        <Toolbar />
        <PatientsPage />
      </Box>
    </Box>
  );
}
