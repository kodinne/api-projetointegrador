import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Product } from './product.entity';
import { CreateProductDto } from './dto/create-product.dto';

@Injectable()
export class ProductsService {
  constructor(
    @InjectRepository(Product)
    private readonly repo: Repository<Product>,
  ) {}

  create(dto: CreateProductDto) {
    const prod = this.repo.create(dto);
    return this.repo.save(prod);
  }

  async findAll(opts?: { page?: number; limit?: number; status?: string; q?: string }) {
    const page = Math.max(1, opts?.page || 1);
    const take = Math.max(1, opts?.limit || 10);
    const skip = (page - 1) * take;
    const qb = this.repo.createQueryBuilder('p').orderBy('p.createdAt', 'DESC').skip(skip).take(take);
    if (opts?.status) qb.andWhere('p.status = :status', { status: opts.status });
    if (opts?.q) {
      const q = `%${opts.q}%`;
      qb.andWhere('(p.name LIKE :q OR p.sku LIKE :q OR p.category LIKE :q)', { q });
    }
    const [items, total] = await qb.getManyAndCount();
    return { items, total, page, limit: take };
  }

  async findOne(id: number) {
    const p = await this.repo.findOne({ where: { id } });
    if (!p) throw new NotFoundException(`Produto com id ${id} não encontrado`);
    return p;
  }

  async updateStock(id: number, stock: number) {
    const p = await this.repo.findOne({ where: { id } });
    if (!p) throw new NotFoundException('Product not found');
    p.stock = stock;
    return this.repo.save(p);
  }

  async remove(id: number): Promise<void> {
    const result = await this.repo.delete(id);
    if (result.affected === 0) {
      throw new NotFoundException(`Produto com id ${id} não encontrado`);
    }
    // retorna void -> controller responderá com 200 por padrão.
  }
}
