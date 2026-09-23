import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'blotter', pathMatch: 'full' },
  {
    path: 'blotter',
    loadComponent: () =>
      import('./features/trade-blotter/trade-blotter.component').then((m) => m.TradeBlotterComponent)
  },
  {
    path: 'positions',
    loadComponent: () =>
      import('./features/positions/positions.component').then((m) => m.PositionsComponent)
  },
  {
    path: 'new-trade',
    loadComponent: () =>
      import('./features/trade-entry/trade-entry.component').then((m) => m.TradeEntryComponent)
  }
];
