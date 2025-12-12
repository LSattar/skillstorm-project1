import './App.css';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { ThemeProvider } from './contexts/ThemeContext';
import { Navbar } from './components/Navbar';
import { Home } from './pages/Home';
import { Companies } from './pages/Companies';
import { Employees } from './pages/Employees';
import { Warehouses } from './pages/Warehouses';
import { Inventory } from './pages/Inventory';

function App() {
  return (
    <ThemeProvider>
      <Router>
        <div className="App">
          <Navbar />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/companies" element={<Companies />} />
            <Route path="/employees" element={<Employees />} />
            <Route path="/warehouses" element={<Warehouses />} />
            <Route path="/inventory" element={<Inventory />} />
          </Routes>
        </div>
      </Router>
    </ThemeProvider>
  );
}

export default App;
