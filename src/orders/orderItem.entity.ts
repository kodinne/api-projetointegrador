import { Entity, PrimaryGeneratedColumn, Column, ManyToOne, JoinColumn } from 'typeorm';
import { Order } from './order.entity';
import { Product } from '../products/product.entity';

@Entity('order_items')
export class OrderItem {
  @PrimaryGeneratedColumn()
  id: number;

  @ManyToOne(() => Order, (order) => order.items)
  @JoinColumn()
  order: Order;

  @ManyToOne(() => Product)
  @JoinColumn()
  product: Product;

  @Column({ default: 1 })
  quantity: number;
}

