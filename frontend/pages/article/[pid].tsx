import marked from "marked";
import { useRouter } from "next/router";
import React from "react";
import useSWR from "swr";

import ArticleMeta from "../../components/article/ArticleMeta";
import CommentList from "../../components/comment/CommentList";
import ErrorMessage from "../../components/common/ErrorMessage";
import ArticleAPI from "../../lib/api/article";
import { Article } from "../../lib/types/articleType";
import { SERVER_BASE_URL } from "../../lib/utils/constant";
import fetcher from "../../lib/utils/fetcher";

const ArticlePage = (initialArticle) => {
  const router = useRouter();
  const {
    query: { pid },
  } = router;

  const {
    data: fetchedArticle,
  } = useSWR(
    initialArticle.notFound
      ? null
      : `${SERVER_BASE_URL}/articles/${encodeURIComponent(String(pid))}`,
    fetcher,
    { initialData: initialArticle }
  );

  const { article }: Article = fetchedArticle || initialArticle;

  if (initialArticle.notFound || !article) {
    return <ErrorMessage message="Article not found" />;
  }

  const markup = {
    __html: marked(article.body, { sanitize: true }),
  };

  return (
    <div className="article-page">
      <div className="banner">
        <div className="container">
          <h1>{article.title}</h1>
          <ArticleMeta article={article} />
        </div>
      </div>
      <div className="container page">
        <div className="row article-content">
          <div className="col-xs-12">
            <div dangerouslySetInnerHTML={markup} />
            <ul className="tag-list">
              {article.tagList.map((tag) => (
                <li key={tag} className="tag-default tag-pill tag-outline">
                  {tag}
                </li>
              ))}
            </ul>
          </div>
        </div>
        <hr />
        <div className="article-actions" />
        <div className="row">
          <div className="col-xs-12 col-md-8 offset-md-2">
            <CommentList />
          </div>
        </div>
      </div>
    </div>
  );
};

ArticlePage.getInitialProps = async ({ query: { pid }, res }) => {
  try {
    const { data } = await ArticleAPI.get(pid);
    return data;
  } catch (error) {
    if (error.response?.status === 404) {
      if (res) {
        res.statusCode = 404;
      }
      return { notFound: true };
    }
    throw error;
  }
};

export default ArticlePage;
