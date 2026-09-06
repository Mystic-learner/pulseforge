const express = require('express');
const cors = require('cors');
const app = express();
const PORT = 8080;

app.use(cors());
app.use(express.json());

// In-memory storage
let runs = [];
let runCounter = 0;

// Health check
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok' });
});

// Get all runs
app.get('/api/runs', (req, res) => {
  res.json(runs);
});

// Create a new run
app.post('/api/runs', (req, res) => {
  const { name, url, method, users, durationSeconds, pattern, headers, body } = req.body;
  
  const newRun = {
    id: `run-${++runCounter}`,
    request: {
      name: name || 'Test Run',
      url: url || 'http://localhost:8081/api/checkout',
      method: method || 'GET',
      users: users || 5,
      durationSeconds: durationSeconds || 10,
      pattern: pattern || 'baseline'
    },
    status: 'QUEUED',
    metrics: {
      requestCount: 0,
      successCount: 0,
      errorCount: 0,
      avgLatencyMs: null,
      p50Ms: null,
      p95Ms: null,
      p99Ms: null,
      throughput: null,
      errorRate: null
    },
    createdAt: new Date().toISOString()
  };
  
  runs.unshift(newRun);
  res.json(newRun);
});

// Start a run
app.post('/api/runs/:id/start', (req, res) => {
  const { id } = req.params;
  const { confirm } = req.query;
  
  if (confirm !== 'true') {
    return res.status(400).json({ error: 'Confirmation required' });
  }
  
  const run = runs.find(r => r.id === id);
  if (!run) {
    return res.status(404).json({ error: 'Run not found' });
  }
  
  run.status = 'RUNNING';
  
  // Simulate run completion after a delay
  setTimeout(() => {
    run.status = 'COMPLETED';
    run.metrics = {
      requestCount: Math.floor(Math.random() * 100) + 50,
      successCount: Math.floor(Math.random() * 90) + 40,
      errorCount: Math.floor(Math.random() * 10),
      avgLatencyMs: Math.floor(Math.random() * 200) + 50,
      p50Ms: Math.floor(Math.random() * 150) + 40,
      p95Ms: Math.floor(Math.random() * 300) + 100,
      p99Ms: Math.floor(Math.random() * 500) + 200,
      throughput: parseFloat((Math.random() * 10 + 5).toFixed(2)),
      errorRate: parseFloat((Math.random() * 0.1).toFixed(3))
    };
  }, 3000);
  
  res.json({ status: 'started' });
});

// Get specific run
app.get('/api/runs/:id', (req, res) => {
  const { id } = req.params;
  const run = runs.find(r => r.id === id);
  
  if (!run) {
    return res.status(404).json({ error: 'Run not found' });
  }
  
  res.json(run);
});

// Get run analysis
app.get('/api/runs/:id/analysis', (req, res) => {
  const { id } = req.params;
  const run = runs.find(r => r.id === id);
  
  if (!run) {
    return res.status(404).json({ error: 'Run not found' });
  }
  
  if (run.status !== 'COMPLETED') {
    return res.status(400).json({ error: 'Run not completed' });
  }
  
  const analysis = {
    diagnosis: run.metrics.errorRate > 0.05 
      ? 'High error rate detected' 
      : 'Performance within acceptable range',
    recommendation: run.metrics.p95Ms > 200
      ? 'Consider optimizing database queries or implementing caching'
      : 'No immediate action required',
    evidence: {
      p95Latency: run.metrics.p95Ms,
      errorRate: run.metrics.errorRate,
      throughput: run.metrics.throughput
    }
  };
  
  res.json(analysis);
});

// Projects endpoint
app.get('/api/projects', (req, res) => {
  res.json([
    {
      id: 'project-1',
      name: 'Acme Platform',
      environments: ['Local', 'Development', 'Staging'],
      targets: [
        {
          id: 'checkout-api',
          name: 'Checkout API',
          url: 'http://localhost:8081/api/checkout',
          safety: 'explicit confirmation'
        }
      ]
    }
  ]);
});

app.listen(PORT, () => {
  console.log(`Mock backend running on http://localhost:${PORT}`);
  console.log('API endpoints available:');
  console.log('  GET  /api/health');
  console.log('  GET  /api/runs');
  console.log('  POST /api/runs');
  console.log('  POST /api/runs/:id/start?confirm=true');
  console.log('  GET  /api/runs/:id');
  console.log('  GET  /api/runs/:id/analysis');
  console.log('  GET  /api/projects');
});
