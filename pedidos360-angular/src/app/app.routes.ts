import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { CatalogComponent } from './catalog/catalog.component';
import { OrdersComponent } from './orders/orders.component';
export const routes: Routes = [
    {
        path: 'protegido',
        canActivate: [MsalGuard],
        loadComponent: () =>
            import('./protegido/protegido')
                .then(m => m.Protegido)
    },
    {
        path: 'orders',
        canActivate: [MsalGuard],
        component: OrdersComponent
    },
    {
        path: 'catalog',
        canActivate: [MsalGuard],
        component: CatalogComponent
    },
    {
        path: '',
        redirectTo: 'catalog',
        pathMatch: 'full'
    }
];