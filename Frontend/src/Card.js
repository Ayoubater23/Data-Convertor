import React, { useState, useRef, useEffect } from 'react';
import { FileText, Upload, Download } from 'lucide-react';
import axios from 'axios';

const Card = () => {
    const [isDragging, setIsDragging] = useState(false);
    const [fileName, setFileName] = useState('');
    const [jsonContent, setJsonContent] = useState(null);
    const [displayedContent, setDisplayedContent] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [typingIndex, setTypingIndex] = useState(0);
    const fileInputRef = useRef(null);

    useEffect(() => {
        if (isLoading) {
            const loadingText = '{\n' +
                '  "status": "converting",\n' +
                '  "progress": "processing document",\n' +
                '  "please": "wait while we analyze your content",\n' +
                '  "estimated_time": "few minutes"\n' +
                '}';

            if (typingIndex < loadingText.length) {
                const isNewline = loadingText[typingIndex] === '\n';
                const isStartOfProperty = loadingText.slice(typingIndex - 2, typingIndex) === '",';

                let delay = 50; // Base delay for normal characters

                if (isNewline) {
                    delay = 200; // Longer pause at line breaks
                } else if (isStartOfProperty) {
                    delay = 300; // Even longer pause between properties
                }

                const timer = setTimeout(() => {
                    setDisplayedContent(prev => prev + loadingText[typingIndex]);
                    setTypingIndex(prev => prev + 1);
                }, delay);

                return () => clearTimeout(timer);
            }
        }
    }, [isLoading, typingIndex]);
    useEffect(() => {
        if (!isLoading) {
            setTypingIndex(0);
            setDisplayedContent('');
        }
    }, [isLoading]);

    useEffect(() => {
        if (jsonContent && !isLoading) {
            const contentToType = typeof jsonContent === 'string'
                ? jsonContent
                : JSON.stringify(JSON.parse(JSON.stringify(jsonContent)), null, 2);

            const lines = contentToType.split('\n');
            let currentLineIndex = 0;
            let currentCharIndex = 0;
            let currentText = '';

            const typeContent = () => {
                if (currentLineIndex < lines.length) {
                    const currentLine = lines[currentLineIndex];

                    if (currentCharIndex < currentLine.length) {
                        currentText += currentLine[currentCharIndex];
                        setDisplayedContent(currentText);
                        currentCharIndex++;
                        setTimeout(typeContent, 5);
                    } else {
                        currentText += '\n';
                        setDisplayedContent(currentText);
                        currentLineIndex++;
                        currentCharIndex = 0;
                        setTimeout(typeContent, 20);
                    }
                }
            };

            typeContent();
        }
    }, [jsonContent, isLoading]);

    const handleDragEnter = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragging(true);
    };

    const handleDragOver = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (!isDragging) setIsDragging(true);
    };

    const handleDragLeave = (e) => {
        e.preventDefault();
        e.stopPropagation();

        const rect = e.currentTarget.getBoundingClientRect();
        const x = e.clientX;
        const y = e.clientY;

        if (x < rect.left || x >= rect.right || y < rect.top || y >= rect.bottom) {
            setIsDragging(false);
        }
    };

    const handleDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragging(false);

        const files = e.dataTransfer.files;
        if (files?.length > 0) {
            processFile(files[0]);
        }
    };

    const handleClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = (e) => {
        const files = e.target.files;
        if (files?.length > 0) {
            processFile(files[0]);
        } else {
            setFileName('');
            setJsonContent(null);
            setError(null);
        }
    };

    const processFile = async (file) => {
        if (file) {
            setFileName(file.name);
            setIsLoading(true);
            setError(null);
            setJsonContent(null);
            setDisplayedContent('');
            setTypingIndex(0);

            const formData = new FormData();
            formData.append('file', file);

            try {
                const response = await axios.post(
                    'http://localhost:8000/api/fileConverter/upload',
                    formData,
                    {
                        headers: {
                            'Content-Type': 'multipart/form-data'
                        },
                        timeout: 300000,
                        onUploadProgress: (progressEvent) => {
                            const percentCompleted = Math.round(
                                (progressEvent.loaded * 100) / progressEvent.total
                            );
                            console.log(`Upload Progress: ${percentCompleted}%`);
                        }
                    }
                );

                if (response.data) {
                    if (response.data.data) {
                        setJsonContent(response.data.data);
                    } else {
                        setJsonContent(response.data);
                    }
                }
            } catch (error) {
                console.error('Error details:', error);

                let errorMessage = 'Error processing file: ';

                if (error.response?.data?.message) {
                    errorMessage += error.response.data.message;
                } else if (error.response?.data?.error) {
                    errorMessage += error.response.data.error;
                } else if (error.message) {
                    errorMessage += error.message;
                }

                setError(errorMessage);
            } finally {
                setIsLoading(false);
            }
        }
    };

    const downloadJson = () => {
        if (jsonContent) {
            const jsonString = typeof jsonContent === 'string'
                ? jsonContent
                : JSON.stringify(jsonContent, null, 2);
            const blob = new Blob([jsonString], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            const baseName = fileName.split('.')[0];
            link.download = `${baseName}.json`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        }
    };

    return (
        <div>
            <div className="card">
                <div className="card-text">
                    <h2>Document to JSON Converter</h2>
                    <h4 className="h4">Upload PDF, DOCX, or image files for conversion</h4>
                </div>
                <div
                    className={`card-form ${isDragging ? 'drag-over' : ''}`}
                    onDragEnter={handleDragEnter}
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    onDrop={handleDrop}
                    onClick={handleClick}
                >
                    <input
                        type="file"
                        ref={fileInputRef}
                        className="hidden"
                        accept=".pdf,.docx,.png,.jpg,.jpeg"
                        onChange={handleFileChange}
                        style={{display: 'none'}}
                    />
                    <Upload className="upload-icon"/>
                    <h3>Drop your file here or click to browse</h3>
                    <h5 className="h4">Supported formats: PDF, DOCX, PNG, JPG</h5>
                </div>
            </div>
            <div className="card2">
                <div className="card2-text">
                    <h4>JSON Preview</h4>
                </div>
                <div className="card2-content">
                    <div className="card2-file">
                        <FileText className="card-file-icon"/>
                        <div className="card2-file-text">
                            {fileName && <p className="font-medium">{fileName}</p>}
                        </div>
                    </div>
                    {(isLoading || jsonContent) ? (
                        <>
                            <div className="json-preview" style={{
                                maxHeight: '300px',
                                overflowY: 'auto',
                                padding: '16px',
                                background: '#f8f9fa',
                                borderRadius: '8px',
                                marginTop: '16px',
                                fontFamily: 'monospace',
                                border: '1px solid #e9ecef'
                            }}>
                <pre style={{ margin: 0 }}>
                  {displayedContent}
                    <span
                        style={{
                            borderRight: '2px solid #4F46E5',
                            animation: 'blink 1s step-end infinite'
                        }}
                    >&nbsp;</span>
                </pre>
                            </div>
                            {!isLoading && jsonContent && (
                                <button
                                    onClick={downloadJson}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        gap: '8px',
                                        marginTop: '16px',
                                        padding: '10px 20px',
                                        backgroundColor: '#4F46E5',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '8px',
                                        fontSize: '16px',
                                        fontWeight: '500',
                                        cursor: 'pointer',
                                        transition: 'all 0.2s ease',
                                        width: 'auto',
                                        boxShadow: '0 2px 4px rgba(0, 0, 0, 0.1)',
                                    }}
                                    onMouseOver={(e) => {
                                        e.currentTarget.style.backgroundColor = '#4338CA';
                                        e.currentTarget.style.transform = 'translateY(-1px)';
                                        e.currentTarget.style.boxShadow = '0 4px 6px rgba(0, 0, 0, 0.1)';
                                    }}
                                    onMouseOut={(e) => {
                                        e.currentTarget.style.backgroundColor = '#4F46E5';
                                        e.currentTarget.style.transform = 'translateY(0)';
                                        e.currentTarget.style.boxShadow = '0 2px 4px rgba(0, 0, 0, 0.1)';
                                    }}
                                >
                                    <Download size={20} />
                                    Download JSON
                                </button>
                            )}
                        </>
                    ) : error ? (
                        <div className="json-preview" style={{
                            padding: '20px',
                            color: 'red',
                            whiteSpace: 'pre-line',
                            background: '#fff5f5',
                            borderRadius: '8px',
                            border: '1px solid #feb2b2'
                        }}>
                            {error}
                        </div>
                    ) : null}
                </div>
            </div>
            <style>
                {`
          @keyframes blink {
            0%, 100% { opacity: 1; }
            50% { opacity: 0; }
          }
        `}
            </style>
        </div>
    );
};

export default Card;