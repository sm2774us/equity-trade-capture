import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatTabsModule],
  template: `
    <mat-toolbar class="bg-trading-panel border-b border-white/10">
      <span class="font-semibold tracking-wide">Systematic Macro &middot; Trade Capture &amp; Risk</span>
      <span class="flex-1"></span>
      <nav class="flex gap-4 text-sm">
        <a routerLink="/blotter" routerLinkActive="text-trading-accent" class="hover:text-trading-accent">Blotter</a>
        <a routerLink="/positions" routerLinkActive="text-trading-accent" class="hover:text-trading-accent">Positions &amp; P&amp;L</a>
        <a routerLink="/new-trade" routerLinkActive="text-trading-accent" class="hover:text-trading-accent">New Trade</a>
      </nav>
    </mat-toolbar>
    <main class="p-6 bg-trading-bg min-h-screen">
      <router-outlet />
    </main>
  `
})
export class AppComponent {}
