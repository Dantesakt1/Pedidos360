import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-kafka',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './kafka.component.html'
})
export class KafkaComponent implements OnInit {
  private apiUrl = 'https://tvkutrhz8e.execute-api.us-east-1.amazonaws.com/api/kafka';

  topics: any[] = [];
  consumerGroups: any[] = [];
  eventsLog: string[] = [];
  mensajeAlerta = '';

  nuevoTopico = {
    name: '',
    partitions: 3,
    replicationFactor: 3,
    retentionMs: '604800000'
  };

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.refrescarTodo();
  }

  refrescarTodo(): void {
    this.http.get<any[]>(`${this.apiUrl}/topics`).subscribe({
      next: (data) => (this.topics = data),
      error: (err) => console.error('Error cargando tópicos Kafka', err)
    });

    this.http.get<any[]>(`${this.apiUrl}/consumer-groups`).subscribe({
      next: (data) => (this.consumerGroups = data),
      error: (err) => console.error('Error cargando consumer groups', err)
    });

    this.http.get<string[]>(`${this.apiUrl}/events-log`).subscribe({
      next: (data) => (this.eventsLog = data),
      error: (err) => console.error('Error cargando log de eventos', err)
    });
  }

  crearTopico(): void {
    if (!this.nuevoTopico.name.trim()) return;
    this.http.post<any>(`${this.apiUrl}/topics`, this.nuevoTopico).subscribe({
      next: (res) => {
        this.mensajeAlerta = res.mensaje;
        this.nuevoTopico.name = '';
        this.refrescarTodo();
      },
      error: (err) => (this.mensajeAlerta = err.error?.message || 'Error al crear tópico')
    });
  }

  aumentarParticiones(topic: any): void {
    const nuevas = Number(topic.partitions) + 1;
    this.http.put<any>(`${this.apiUrl}/topics/${topic.name}`, { partitions: nuevas }).subscribe({
      next: (res) => {
        this.mensajeAlerta = res.mensaje;
        this.refrescarTodo();
      }
    });
  }

  cambiarRetencion(topic: any, ms: string): void {
    this.http.put<any>(`${this.apiUrl}/topics/${topic.name}`, { retentionMs: ms }).subscribe({
      next: (res) => {
        this.mensajeAlerta = res.mensaje;
        this.refrescarTodo();
      }
    });
  }

  eliminarTopico(name: string): void {
    this.http.delete<any>(`${this.apiUrl}/topics/${name}`).subscribe({
      next: (res) => {
        this.mensajeAlerta = res.mensaje;
        this.refrescarTodo();
      }
    });
  }

  enviarPrueba(canal: string, simularFallo: boolean): void {
    this.http.post<any>(`${this.apiUrl}/test-event`, { canal, simularFallo }).subscribe({
      next: (res) => {
        this.mensajeAlerta = res.mensaje;
        setTimeout(() => this.refrescarTodo(), simularFallo ? 7500 : 1200);
      }
    });
  }
}