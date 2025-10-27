import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Order } from './order.entity';
import { OrderItem } from './orderItem.entity';
import { CreateOrderDto } from './dto/create-order.dto';
import { User } from '../users/user.entity';
import { Product } from '../products/product.entity';

@Injectable()
export class OrdersService {
  constructor(
    @InjectRepository(Order) private readonly ordersRepo: Repository<Order>,
    @InjectRepository(OrderItem) private readonly itemsRepo: Repository<OrderItem>,
    @InjectRepository(User) private readonly usersRepo: Repository<User>,
    @InjectRepository(Product) private readonly productsRepo: Repository<Product>,
  ) {}

  async create(dto: CreateOrderDto) {
    const customer = await this.usersRepo.findOne({ where: { id: dto.customerId } });
    const order = this.ordersRepo.create({
      customer: customer!,
      salesChannel: dto.salesChannel ?? 'sales',
      destination: dto.destination ?? 'warehouse',
    });
    order.items = [];
    for (const item of dto.items) {
      const product = await this.productsRepo.findOne({ where: { id: item.productId } });
      const orderItem = this.itemsRepo.create({ product: product!, quantity: item.quantity });
      order.items.push(orderItem);
      if (product) {
        product.stock = Math.max(0, product.stock - item.quantity);
        await this.productsRepo.save(product);
      }
    }
    return this.ordersRepo.save(order);
  }

  async findAll(opts: { page: number; limit: number; status?: string; q?: string }) {
    const page = Math.max(1, opts.page || 1);
    const take = Math.max(1, opts.limit || 10);
    const skip = (page - 1) * take;

    const qb = this.ordersRepo.createQueryBuilder('o')
      .leftJoinAndSelect('o.customer', 'customer')
      .leftJoinAndSelect('o.items', 'items')
      .leftJoinAndSelect('items.product', 'product')
      .orderBy('o.createdAt', 'DESC')
      .skip(skip)
      .take(take);

    if (opts.status) qb.andWhere('o.status = :status', { status: opts.status });
    if (opts.q) {
      const q = `%${opts.q}%`;
      qb.andWhere('(customer.name LIKE :q OR o.destination LIKE :q OR CAST(o.id as char) LIKE :q)', { q });
    }

    const [items, total] = await qb.getManyAndCount();
    return { items, total, page, limit: take };
  }
}
