import { Entity, PrimaryGeneratedColumn, Column, CreateDateColumn, ManyToOne, OneToMany, JoinColumn } from 'typeorm';
import { User } from '../users/user.entity';
import { OrderItem } from './orderItem.entity';

@Entity('orders')
export class Order {
  @PrimaryGeneratedColumn()
  id: number;

  @ManyToOne(() => User)
  @JoinColumn()
  customer: User;

  @Column({ default: 'sales' })
  salesChannel: string;

  @Column({ default: 'warehouse' })
  destination: string;

  @CreateDateColumn()
  createdAt: Date;

  @Column({ default: 'pending' })
  status: string;

  @OneToMany(() => OrderItem, (item) => item.order, { cascade: true })
  items: OrderItem[];
}

