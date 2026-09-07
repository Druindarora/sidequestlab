import { defineCollection, z } from 'astro:content';
import { glob } from 'astro/loaders';

const workflows = defineCollection({
  loader: glob({ base: './src/content/workflows', pattern: '**/*.md' }),
  schema: z.object({
    title: z.string().min(1),
    navTitle: z.string().min(1),
    summary: z.string().min(1),
    workflowId: z.string().regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
    workflowSlug: z.string().regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/),
    version: z.string().regex(/^v\d+$/),
    publishedAt: z.coerce.date(),
    status: z.enum(['draft', 'published']),
    order: z.number().int().nonnegative(),
  }),
});

export const collections = { workflows };
